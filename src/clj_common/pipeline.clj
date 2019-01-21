(ns clj-common.pipeline
  (:require
   [clojure.core.async :as async]
   [clj-common.context :as context]
   [clj-common.edn :as edn]
   [clj-common.localfs :as fs]))

;;; todo
;;; stopping chain is not working, close of underneath chan would not stop reader
;;; since reader buffers elements ...
;;; meaning take is not functioning as expected


(defn read-edn-go
  "Reads contents of file to given channel. Channel is closed when file is read."
  [context path ch]
  (async/go     
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

(defn write-edn-go
  "Writes contents of given channel. File is closed when channel is closed."
  [context path ch]
  (async/go
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
      :success)))

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
    (async/close! in)
    (async/close! out)
    (context/set-state context "completion")))

(defn filter-go
  "To be replaced with single combining transducer once I learn how to setup it."
  [context in filter-fn out]
  (async/go
    (context/set-state context "init")
    (loop [object (async/<! in)]
      (context/set-state context "step")
      (when object
        (context/counter context "in")
        (when (filter-fn object)
          (async/>! out)
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
      (transducer-fn)
      (loop [object (async/<! in)]
        (context/set-state context "step")
        (when object
          (context/counter context "in")
          (when-let [result (transducer-fn nil object)]
            (async/>! out result)
            (context/counter context "out"))
          (recur (async/<! in)))
        )
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
            (async/close! out)
            (async/>! out (reducing-fn state))
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

(defn trace-go
  "Intented for debugging. Reports to context all messages that go over channel,
  either raw or result of applying fn to raw."
  ([context in]
   (trace-go context in identity nil))
  ([context in out]
   (trace-go context in identity out))
  ([context in fn out]
   (async/go
     (context/set-state context "trace-init")
     (loop [message (async/<! in)]
       (if message
         (do
           (context/set-state context "trace-step")
           (context/trace context (fn message))
           (context/counter context "trace")
           (when out
             (async/>! out message))
           (recur (async/<! in)))
         (do
           (when out
             (async/close! out))
           (context/set-state context "trace-completion"))))
     :success)))

