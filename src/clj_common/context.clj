(ns clj-common.context)

;;; context v2, not same as one in maply-backend-tools

(def context
  {
   :counter-fn [:fn :string nil]
   :state-fn [:fn :object :nil]
   :trace-fn [:fn :string :nil]
   ;; called when processing fails with exception
   :error-fn [:fn :throwable :object :nil]})

(defn create-stdout-context
  "Creates plain std out context, to be used with sample processing"
  []
  {
   :counter-fn (fn [counter]
                 (println "counter increase " counter))
   :trace-fn (fn [trace]
               (println trace))
   :state-fn (fn [state]
               (println "state set " state))})

(defn create-state-context
  "Creates state based context, adds :context-dump-fn which returns current state of
  context and :context-print-fn which outputs context state. Exception will be re thrown."
  []
  (let [context (atom {})]
    {
     :counter-fn (fn [counter]
                   (swap!
                    context
                    update-in [:counters counter]
                    (fn [value] (inc (or value 0)))))
     :state-fn (fn [state]
                 (swap!
                  context
                  update-in [:state]
                  (fn [_] state)))
     :trace-fn (fn [trace]
                 ;; todo store trace in ring buffer
                 (println trace)
                 )
     :context-dump-fn (fn [] @context) 
     :context-print-fn (fn []
                         (let [state @context]
                           (println "state" (:state state))
                           (println "counters:")
                           (doseq [[counter value] (:counters state)]
                             (println "\t" counter "=" value))))}))

(def ^:dynamic *context* (create-stdout-context))

(defn increment-counter [counter] ((:counter-fn *context*) counter))

(defn trace [trace] ((:trace-fn *context*) trace))

(defn set-state [state] ((:state-fn *context*) state))

(defn error [throwable data]
  ((:error-fn *context*) throwable data))

(defn create-state-context-reporting-thread
  "Creates, starts and returns thread that will on given interval in ms report context state"
  [context reporting-interval-millis]
  (let [thread (new
                Thread
                (fn []
                  (.setName (Thread/currentThread) "context-reporting-thread")
                  (try
                    (while
                       true
                       ((:context-print-fn context))
                       (Thread/sleep reporting-interval-millis))
                    (catch InterruptedException e ((:context-print-fn context))))))]
    (.start thread)
    thread))

#_(let [counter (atom 0)]
  (try
   (while
       true
       (if (= @counter 3) (throw (new Exception "Test") ) )
       (swap! counter inc)
       (Thread/sleep 1000))
   (catch Exception e (println "finish"))))

#_(let [context (create-state-context)]
  (transduce
   identity
   (create-context-reducing-fn context list-reducing-fn)
   [1 2 3])
  ((:context-print-fn context)))

#_((create-context-reducing-fn (create-stdout-context) list-reducing-fn) [] 2)
#_(let [context (create-state-context)]
  ((:counter-fn context) "test")
  ((:counter-fn context) "test")
  ((:context-print-fn context)))

#_(update-in {} [:counters :a] (fn [value] (inc (or value 0))))
#_(update-in {:counters {:a 5}} [:counters :a] (fn [value] (inc (or value 0))))

#_@(atom {} )

#_(Transduce
 identity
 (fn
   ([] (println "init") [])
   ([state object] (println "iteration") (conj state object))
   ([state] (println "close")))
 [1 2 3])

#_(transduce
 identity
 list-concat-reducing-fn
 [[1 2 3] [4 5 6] [7]])
