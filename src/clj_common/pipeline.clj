(ns clj-common.pipeline
  (:use
   clj-common.clojure)
  (:require
   [clojure.core.async :as async]
   [clj-common.context :as context]
   [clj-common.edn :as edn]
   [clj-common.env :as env]
   [clj-common.localfs :as fs]
   [clj-common.io :as io]
   [clj-common.json :as json]
   [clj-common.jvm :as jvm]
   [clj-common.path :as path]))

;;; concept
;;; chan - connection point
;;; go - processing power, has at least in and out chan as params
;;; required features:
;;; - ability to stop processing
;;; - metrics, number of entries read, emitted
;;;   ( it would be good to be able to say for each filter, map, reduce stats 
;;; notes:
;;; normal flow, read go emits data, transducer go does processing, write go saves to fs
;;; read go will close out channel once all data is read
;;; write go will, once in channel is closed, close stream to fs
;;; stopping, each go should close in channel in case it's unable to write to out channel
;;; pipeline halt could happen in both directions, downstream normal and upstream stopped

;;; go routine
;;; scope of execution, defined with context, in, out channels, state and side effects
;;; has one of four states: init, step, exception, completion
;;; once exception state is entered processing should halt


;;; todo
;;; exception handling, maybe each go should have try catch and on exception close both in and
;;; out channels

;;; todo
;;; maybe it makes sense to do all IO operations in simple, isolated gos, in that way throttling
;;; can be easily added later 

;;; todo
;;; create some construct which would catch exception inside go and report go with state
;;; set to "exception" that would help debugging, maybe even close pipeline once it happens

;;; todo
;;; controller - way to sequence pipelines together
;;; either to use in namespaces to execute one pipeline at the time or to sync multiple
;;  pipelines, maybe to use completion state of nodes to know when execution is finished 

;;; todo
;;; 20190209, unified external resource allocation ( input / output streams ), issue with
;;; connecting to channels, side effect gos,
;;; example: trek-mate.integration.osm/create-dot-split-tile-dynamic-out-fn

;;; todo
;;; stopping chain is not working, close of underneath chan would not stop reader
;;; since reader buffers elements ...
;;; meaning take is not functioning as expected
;;; 20190127, update, close-and-exaust should be used within all stopping go-s, note tested
;;; 20190130, each go should close and drain in channel in case it was unable to write to
;;; out channel, implemented in chunk-to-map-go

;;; todo
;;; return of gos, currently in some it's :sucesss, should it be channel to stop go or
;;; some control thing ...

(defn closed? [channel]
  (clojure.core.async.impl.protocols/closed? channel))

(defmacro close-and-exhaust
  "To be used within pipeline to close and exhaust channel. Exhaust is important
  for stopping of pipeline, read -> take example, once take obtained enough elements.
  It will close channel read is emitting to and read one more element read created.
  Assumes it's being called inside go loop."
  [chan]
  `(do
     (async/close! ~chan)
     (loop [element# (async/<! ~chan)]
       (when element#
         (recur (async/<! ~chan))))))

(defn stop-pipeline [channel]
  (async/go
    (close-and-exhaust channel)))


(defmacro out-or-close-and-exhaust-in
  "Tries to write to out channel. If out is closed closes in channel and reads all data
  from it. Assumes it's being called inside go loop."
  [out data in-to-close]
  `(if (async/>! ~out ~data)
     true
     (do
       (close-and-exhaust ~in-to-close)
       false)))

(defn channel->seq
  "Reads whole channel into seq. Blocking. Current implementation reads whole channel into memory."
  [ch]
  (loop [data (list)]
    (if-let [message (async/<!! ch)]
      (recur (conj data message))
      (reverse data))))

(defn wait-on-channel
  "Waits for value on given channel for millis before returning nil. Timeout is added to ensure
  calling thread would not be locked indefinetly."
  [context chan timeout-millis]
  (context/set-state context "init")
  (context/set-state context "step")
  (let [return (async/alt!!
                 (async/timeout timeout-millis) nil
                 chan ([v chan] v))]
    (context/set-state context "completion")
    return))

;;; helper channels provider, to enable dynamic allocation and binding of channels
;;; take a look to trek-mate.examples.belgrade for usage cases
(defn create-channels-provider
  "Creates provider of channels, to be called with keyword representing channel, if
  channel doesn't exist in pool one will be created, could be used at end to verify
  pipeline is finished.
  Returns function with two arities, arity 0 returns current state of channels, arity
  1 ensures channel with given keyword is created."
  []
  (let [channels (atom {})]
    (fn
      ([] @channels)
      ([channel-keyword]
       (if-let [channel (get @channels channel-keyword)]
         channel
         (get
          (swap!
           channels
           (fn [channels]
             (if-let [channel (get channels channel-keyword)]
               channels
               (assoc channels channel-keyword (async/chan)))))
          channel-keyword))))))

;;; state context reporting on fixed interval until at least one channel is open
(defn create-state-context-reporting-finite-thread
  "Creates, starts and returns thread that will on given interval in ms report context state"
  [context reporting-interval-millis]
  (let [thread (new
                Thread
                (fn []
                  (.setName (Thread/currentThread) "context-reporting-thread")
                  (try
                    (loop []
                      ((:context-print-fn context))
                      (let [context-dump (:state ((:context-dump-fn context)))]
                        (when
                           (or
                            (= (count context-dump) 0)
                            (some?
                             (seq
                              (filter (complement #(= % "completion")) (vals context-dump)))))
                           (do
                             (Thread/sleep reporting-interval-millis)
                             (recur)))))
                    (report "pipeline finished")
                    ((:context-print-fn context))
                    (catch InterruptedException e ((:context-print-fn context))))))]
    (.start thread)
    thread))

;;; resource countroller concept
;;; to be used to control all stateful resources ( local fs, remote fs, cloudkit )
;;; content of entry in theory could be channel which when closed would stop reading / writing
;;; resource controller is defined as function with 3 arities
;;; (resource-control) -> returns currently open resouces
;;; (resource-control path go-fn channel) -> reports resource usage
;;; (resource-control path go-fn) -> removes resource usage
(defn create-resource-controller
  "Creates resource controller to be used inside go fns working with resources"
  [context]
  (let [data (atom {})]
    (fn
      ([] (deref data))
      ([path go-fn channel]
       ;;; todo notify if multiple go fns use same resource
       (swap! data assoc path [go-fn channel]))
      ([path go-fn]
       (swap! data dissoc path)))))

(defn create-dummy-resource-controller
  "Creates dummy resource controller to be used when resource tracking is not required"
  []
  (fn
    ([])
    ([_ _ _])
    ([_ _])))

(defn create-trace-resource-controller
  [context]
  (fn
    ([])
    ([path go _] (context/trace context (str "open" path "by" go)))
    ([path go] (context/trace context (str "close" path "by" go)))))

(defn read-line-go
  "Reads contents of file, line by line to given channel. Channel is closed when file is read.
  In case ch is closed by downstream reading is stopped. Reports reading to resource controller"
  [context resource-control path ch]
  (async/go
    (with-open [input-stream (fs/input-stream path)]
      (context/set-state context "init")
      (resource-control path read-line-go ch)
      (loop [line-seq (io/input-stream->line-seq input-stream)]
        (when-let [line (first line-seq)] 
          (context/set-state context "step")
          (when (async/>! ch line)
            (context/counter context "read")
            (recur (rest line-seq))))))
    (resource-control path read-line-go)
    (async/close! ch)
    (context/set-state context "completion")))

(defn read-line-directory-go
  [context resource-control directory prefix ch]
  (async/go
    (context/set-state context "init")
    (doseq [path (filter
                  #(.startsWith (path/name %) prefix)
                  (fs/list directory))]
      (context/set-state context "step")
      (context/increment-counter context "processing-file")
      (with-open [input-stream (fs/input-stream path)]
        (resource-control path read-line-go ch)
        (loop [line-seq (io/input-stream->line-seq input-stream)]
          (when-let [line (first line-seq)] 
            (when (async/>! ch line)
              (context/counter context "read")
              (recur (rest line-seq))))))
      (resource-control path read-line-go)
      (context/increment-counter context "processed-file"))
    (async/close! ch)
    (context/set-state context "completion")))

(defn read-json-path-seq-go
  [context resource-controller path-seq ch]
  (async/go
    (context/set-state context "init")
    (loop [path (first path-seq)
           path-seq (rest path-seq)]
      (when path
        (context/set-state context "step")
        (context/increment-counter context "processing-file")
        (let [data (with-open [is (fs/input-stream path)]
                     (resource-controller path read-json-path-seq-go ch)
                     (json/read-keyworded is))]
          (resource-controller path read-json-path-seq-go)  
          (when (async/>! ch data)
            (context/increment-counter context "processed-file")
            (recur (first path-seq) (rest path-seq))))))
    (async/close! ch)
    (context/set-state context "completion")))

(defn read-line-from-input-stream-go
  "Reads line by line from given input stream. Each line is sent to given channel. In channel
  is closed reading is stopped. Closes input stream when done."
  [context input-stream ch]
  (async/go
    (context/set-state context "init")
    (with-open [reader (io/input-stream->buffered-reader input-stream)]
      (loop [line (io/read-line reader)]
        (when line
          (context/set-state context "step")
          (when (async/>! ch line)
            (context/counter context "read")
            (recur (io/read-line reader)))))
      (async/close! ch)
      (context/set-state context "completion"))))

(declare transducer-stream-go)

;;; depricated use read-line-go with transducer transforming to edn
(defn read-edn-go
  "Reads contents of file to given channel. Channel is closed when file is read."
  ;;; depricated use version with resource controller
  ([context path ch] (read-edn-go context (create-dummy-resource-controller) path ch))
  ([context resource-control path ch]
   (let [in (async/chan)]
     (read-line-go context resource-control path in)
     (transducer-stream-go
      (context/wrap-scope context "edn")
      in
      (map edn/read)
      ch)))
  
  ;;; old implementation
  #_(async/go     
    (with-open [input-stream (fs/input-stream path)]
      #_(context/counter context "init")
      (context/set-state context "init")
      (loop [objects (edn/input-stream->seq input-stream)]
        (when-let [object (first objects)]
          (let [success (async/>! ch object)]
            (context/set-state context "step")
            (when success
              (context/counter context "in")
              (recur (rest objects))))))
      (async/close! ch)
      #_(context/counter context "completion")
      (context/set-state context "completion")
      :success)))

(defn write-line-go
  [context resource-control path ch]
  (async/go
    (with-open [output-stream (fs/output-stream path)]
      (context/set-state context "init")
      (resource-control path write-line-go ch)
      (loop [line (async/<! ch)]
        (context/set-state context "step")
        (when line
          (io/write-line output-stream line)
          (context/counter context "write")
          (recur (async/<! ch)))))
    (resource-control path write-line-go)
    (context/set-state context "completion")))

(defn write-line-atomic-go
  "Writes data to temporary location, once channel is closed data is moved to final location"
  [context resource-control path ch]
  (let [temp-path (path/child env/*temp-path* (jvm/random-uuid))]
    (async/go
      (with-open [output-stream (fs/output-stream temp-path)]
        (context/set-state context "init")
        (resource-control temp-path write-line-atomic-go ch)
        (loop [line (async/<! ch)]
          (context/set-state context "step")
          (when line
            (io/write-line output-stream line)
            (context/counter context "write")
            (recur (async/<! ch)))))
      (resource-control temp-path write-line-atomic-go)
      (fs/move temp-path path)
      (context/set-state context "completion"))))

;;; depricated use write-line with transducer transforming to string
(defn write-edn-go
  "Writes contents of given channel. File is closed when channel is closed."
  ([context path ch]
   (write-edn-go context (create-dummy-resource-controller) path ch))
  ([context resource-control path ch]
   (let [out (async/chan)]
     (transducer-stream-go
      (context/wrap-scope context "edn")
      ch
      (map edn/write-object)
      out)
     (write-line-go context resource-control path out))

   ;;; old implementation
   #_(async/go
     (with-open [output-stream (fs/output-stream path)]
       #_(context/counter context "init")
       (context/set-state context "init")
       (loop [object (async/<! ch)]
         (when object
           (edn/write-object output-stream object)
           (context/counter context "write")
           (context/set-state context "step")
           (recur (async/<! ch))))
       #_(context/counter context "completion")
       (context/set-state context "completion")
       :success))))

(defn emit-var-seq-go
  "Emits sequence stored in given variable to channel. Channel is closed when sequence
  is fully iterated."
  [context var ch]
  (async/go
    (context/set-state context "init")
    (loop [elements (deref var)]
      (when-let [element (first elements)]
        (let [success (async/>! ch element)]
          (context/set-state context "step")
          (when success
            (context/counter context "emit")
            (recur (rest elements))))))
    (async/close! ch)
    (context/set-state context "completion")
    :success))

(defn emit-seq-go
  "Emits sequence stored in given variable to channel. Channel is closed when sequence
  is fully iterated."
  [context data-seq ch]
  (async/go
    (context/set-state context "init")
    (loop [elements data-seq]
      (when-let [element (first elements)]
        (let [success (async/>! ch element)]
          (context/set-state context "step")
          (when success
            (context/counter context "emit")
            (recur (rest elements))))))
    (async/close! ch)
    (context/set-state context "completion")
    :success))

(defn capture-var-go
  "Intented for debugging. Captures object from channel into given root variable.
  Note: for capture of sequence use capture-seq-go."
  [context in var]
  (async/go
    (context/set-state context "capture-init")
    (let [object (async/<! in)]
      (alter-var-root var (constantly object)))
    (context/set-state context "capture-completion")
    :success))

(defn capture-var-seq-go
  "Captures sequence of objects coming from stream to given var. Not atomic, updates
  variable on each object"
  [context in var]
  (async/go
    (context/set-state context "init")
    (alter-var-root var (constantly []))
    (loop [object (async/<! in)]
      (context/set-state context "step")
      (when object
        (context/counter context "in")
        (alter-var-root var conj object)
        (recur (async/<! in))))
    (context/set-state context "completion")
    :success))

(defn capture-var-seq-atomic-go
  "Atomic version of capture-var-seq-go, stores objects internally and updates var on
  channel close."
  [context in var]
  (async/go
    (context/set-state context "init")
    (loop [state (list)
           object (async/<! in)]
      (context/set-state context "step")
      (if object
        (do
          (context/counter context "in")
          (recur (conj state object) (async/<! in)))
        (alter-var-root var (constantly (reverse state)))))
    (context/set-state context "completion")
    :success))

(defn capture-atom-seq-go
  "Captures sequence of objects coming from stream to given atom. Not atomic, updates
  atom on each object"
  [context in atom]
  (async/go
    (context/set-state context "init")
    (swap! atom (constantly []))
    (loop [object (async/<! in)]
      (context/set-state context "step")
      (when object
        (context/counter context "in")
        (swap! atom conj object)
        (recur (async/<! in))))
    (context/set-state context "completion")
    :success))


(defn broadcast-go
  "Broadcasts messages from channel to multiple channels. Synchronously."
  [context in & outs]
  (async/go
    (context/set-state context "init")
    (loop [message (async/<! in)]
      (context/set-state context "step")
      (when message
        (context/counter context "in")
        (doseq [out outs]
          (async/>! out message)
          (context/counter context "out"))
        (recur (async/<! in))))
    (doseq [out outs]
      (async/close! out))
    (context/set-state context "completion")
    :success))

(defn drain-go
  "Intented to be connected to reducing-go, will keep only latest state produced by
  reducing fn, once in channel is closed final state, if any, will be sent to out
  channel and out channel will be closed."
  [context in out]
  (async/go
    #_(context/counter context "init")
    (context/set-state context "init")
    (loop [state nil
           new-state (async/<! in)]
      (if new-state
        (do
          (context/counter context "in")
          (context/set-state context "step")
          (recur new-state (async/<! in)))
        (do
          (when state
            (do
              (async/>! out state)
              (context/counter context "out")))
          (async/close! out)
          #_(context/counter context "completion")
          (context/set-state context "completion"))))
    :success))

(defn ignore-close-go
  "Copies data from in to out, ignoring propagation of close. To be used when multiple results
  need to be reported to single controller."
  [context in out]
  (async/go
    (context/set-state "init")
    (loop [object (async/<! in)]
      (when object
        (context/set-state "step")
        (context/counter "in")
        (when (out-or-close-and-exhaust-in out object in)
          (context/counter "out")
          (recur (async/<! in)))))
    (context/set-state "completion")))

(defn take-go
  "Emits given number of elements from in chan to out chan. When required number of elements
  is obtained closes in chan."
  [context count in out]
  (async/go
    (context/set-state context "init")
    (loop [left count
           element (async/<! in)]
      (when element
        (context/set-state context "step")
        (context/counter context "read")
        (when (async/>! out element)
          (when (> left 1)
            (recur (dec left) (async/<! in))))))
    (close-and-exhaust in)
    (async/close! out)
    (context/set-state context "completion")))

(defn chunk-to-map-go
  "Reads given number of elements from in ch, adds them to map by given key-fn. Once chunk
  is read map is sent downstream. Not waiting for downstream to process chunk before creating
  next one"
  [context in key-fn value-fn chunk-size out]
  (async/go
    (context/set-state context "init")
    (loop [chunk {}
           count 0
           element (async/<! in)]
      (if element
        (do
          (context/set-state context "step")
          (context/counter context "in")
          (let [new-chunk (assoc chunk (key-fn element) (value-fn element))
                new-count (inc count)]
            (if (= new-count chunk-size)
              (do
                (when (out-or-close-and-exhaust-in out new-chunk in)
                  (context/counter context "out")
                  (recur {} 0 (async/<! in))))
              (recur new-chunk new-count (async/<! in)))))
        (when (> count 0)
          (when (out-or-close-and-exhaust-in out chunk in)
            (context/counter context "out")))))
    (async/close! out)
    (context/set-state context "completion")))

(defn constantly-go
  "Reads value from in channel once and emitts same value to out until out is not closed"
  [context in out]
  (async/go
    (context/set-state context "init")
    (let [value (async/<! in)]
      (when value
        (context/set-state context "step")
        (context/counter context "in")
        (loop [result (async/>! out value)]
          (when result
            (context/counter context "out")
            (recur (async/>! out value))))
        (context/set-state context "completion")))))

;; not sure is working, use transducer-stream-go
#_(defn filter-go
  "To be replaced with single combining transducer once I learn how to setup it."
  [context in filter-fn out]
  (async/go
    (context/set-state context "init")
    (loop [object (async/<! in)]
      (context/set-state context "step")
      (when object
        (context/counter context "in")
        (when (filter-fn object)
          (async/>! out object)
          (context/counter context "out"))
        (recur (async/<! in))))
    (async/close! out)
    (context/set-state context "completion")))

(defn transducer-stream-go
  "Support for sequence transducer ( map, filter ... )"
  [context in transducer out]
  (let [transducer-fn (transducer
                       (fn
                         ([] nil) 
                         ([state entry] entry)
                         ([state] nil)))]
    (async/go
      (context/set-state context "init")
      ;; doesn't have effect
      (transducer-fn)
      (loop [object (async/<! in)]
        (context/set-state context "step")
        (when object
          (context/counter context "in")
          (if-let [result (binding [context/*context* context]
                            (transducer-fn nil object))]
            (when (out-or-close-and-exhaust-in out result in)
              (context/counter context "out")
              (recur (async/<! in)))
            (recur (async/<! in)))))
      ;; doesn't have effect
      (transducer nil)
      (async/close! out)
      (context/set-state context "completion")
      :success)))

(defn transducer-stream-list-go
  "Support for sequence transducer ( mapcat, filter ... )"
  [context in transducer out]
  (let [transducer-fn (transducer
                       (fn
                         ([] nil) 
                         ([state entry] entry)
                         ([state] nil)))]
    (async/go
      (context/set-state context "init")
      ;; doesn't have effect
      (transducer-fn)
      (loop [object (async/<! in)]
        (context/set-state context "step")
        (when object
          (context/counter context "in")
          (if-let [result-seq (binding [context/*context* context]
                                (transducer-fn nil object))]
            (loop [result-seq result-seq]
              (when-let [result (first result-seq)]
                (when (out-or-close-and-exhaust-in out result in)
                  (context/counter context "out"))
                (recur (rest result-seq)))))
          (recur (async/<! in))))
      ;; doesn't have effect
      (transducer nil)
      (async/close! out)
      (context/set-state context "completion")
      :success)))

(defn reducing-go
  "Performs given reducing function over read elements from in channel and emits state
  to given out channel in each step. When reduction is finished result of completion
  is sent to out channel. Out channel is closed at the end.
  reducing fn
     [] init, returns initial state
     [state entry] step, performs reduction and returns new state
     [state] completion, performs final modification of state, returns final state"
  [context in reducing-fn out]
  (async/go
    (let [initial-state (reducing-fn)]
      #_(context/counter context "init")
      (context/set-state context "init")
      (loop [state initial-state
             input (async/<! in)]
        (if input
          (do
            (context/counter context "in")
            (context/set-state context "step")
            (let [new-state (reducing-fn state input)]
              (async/>! out new-state)
              (context/counter context "out")
              (recur new-state (async/<! in))))
          (do
            (async/>! out (reducing-fn state))
            (async/close! out)
            #_(context/counter context "completion")
            (context/set-state context "completion")))))
    :success))

(defn side-effect-reducing-go
  "Performs side effect reducing function over elements retrieved from channel. To be used
  when reducing function completion step is responsable for reduction result."
  [context in reducing-fn]
  (async/go
    (let [initial-state (reducing-fn)]
      (context/set-state context "init")
      (loop [state initial-state
             input (async/<! in)]
        (if input
          (do
            (context/counter context "in")
            (context/set-state context "step")
            (let [new-state (reducing-fn state input)]
              (recur new-state (async/<! in))))
          (do
            (reducing-fn state)
            (context/set-state context "completion")))))))

(defn for-each-go
  [context in fn-to-apply]
  (async/go
    (context/set-state context "init")
    (loop [element (async/<! in)]
      (when element
        (context/set-state context "step")
        (context/counter context "in")
        (fn-to-apply element)
        (recur (async/<! in))))
    (context/set-state context "completion")))

(defn ignore-stopping-go
  "Intended for debugging. Enables connection of pipelines which are done on chunk of data
  to be connected with channel which streams data to debug upon.
  Note: current implementation looses one element on each change of downstream pipeline"
  [context in out]
  (async/go
    (context/set-state "init")
    (loop [element (async/<! in)]
      (when element
        (context/set-state context "step")
        (context/counter context "in")
        ;;; todo
        ;;; element should be returned to when unable to write it downstream
        (when (async/>! out element)
          (context/counter context "out")
          (recur (async/<! in)))))
    (async/close! out)
    (context/set-state context "completion")))

(defn trace-go
  "Intented for debugging. Reports to context all messages that go over channel,
  either raw or result of applying fn to raw."
  ([context in]
   (trace-go context in identity nil))
  ([context in out]
   (trace-go context in identity out))
  ([context in fn out]
   (async/go
     (context/set-state context "init")
     (loop [message (async/<! in)]
       (if message
         (do
           (context/set-state context "step")
           (context/trace context (fn message))
           (context/counter context "trace")
           (when out
             (async/>! out message))
           (recur (async/<! in)))
         (do
           (when out
             (async/close! out))
           (context/set-state context "completion"))))
     :success)))

(defn funnel-go
  "Copies data from multiple ins to out. Closes when all ins are closed."
  [context in-seq out]
  (async/go
    (context/set-state context "init")
    (loop [in-set (into #{} in-seq)]
      (when (seq in-set)
        (let [[val port] (async/alts! (into []  in-set))]
          (context/set-state context "step")
          (if (some? val)
            (do
              (context/counter context "in")
              (when (async/>! out val)
                (context/counter context "out")
                (recur in-set)))
            (do
              (context/counter context "in-close")
              (recur (disj in-set port)))))))
    (doseq [ch in-seq] (async/close! ch))
    (async/close! out)
    (context/set-state context "completion")))

(defn after-fn-go
  "Executes given zero arity fn when input is closed. Propagates input to output"
  [context in after-fn out]
  (async/go
    (context/set-state context "init")
    (loop [data (async/<! in)]
      (when data
        (context/set-state context "step")
        (context/counter context "in")
        (when (async/>! out data)
          (context/counter context "out")
          (recur (async/<! in)))))
    (async/close! out)
    (context/set-state context "completion")
    (after-fn)))

(defn pass-last-go
  "Propagates only last value to output."
  [context in out]
  (async/go
    (context/set-state context "init")
    (let [last (loop [data (async/<! in)
                      previous nil]
                 (if data
                   (do
                     (context/set-state context "step")
                     (context/counter context "in")
                     (recur (async/<! in) data))
                   previous))]
      (when last
        (when (async/>! out last)
          (context/counter context "out"))))
    (async/close! out)
    (context/set-state context "completion")))

(defn nil-go
  "Reads input and discards it."
  ([context in]
   (async/go
     (context/set-state context "init")
     (loop [message (async/<! in)]
       (if message
         (do
           (context/set-state context "step")
           (context/counter context "in")
           (recur (async/<! in)))
         (context/set-state context "completion")))
     :success)))

(defn create-lookup-go
  "Reads objects from in, aggregates them in map using key-fn to calculate
  key and result of value-fn on object as value. Once in is closed created
  map is sent to out"
  [context in key-fn value-fn out]
  (async/go
    (context/set-state context "init")
    (loop [lookup {}
           object (async/<! in)]
      (if object
        (do
          (context/set-state context "step")
          (context/increment-counter context "in")
          (recur
           (assoc lookup (key-fn object) (value-fn object))
           (async/<! in)))
        (do
          (async/>! out lookup)
          (context/increment-counter context "out")
          (async/close! out)
          (context/set-state context "completion"))))))

