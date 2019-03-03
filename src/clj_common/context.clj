(ns clj-common.context)

;;; context v2, not same as one in maply-backend-tools
;;; dynamic binding variant is good for single thread processing, when using
;;; core.async context will be provided

;;; different context impementations should implement only scope* fns, extending
;;; base context map ...

(def context
  {
   ;; prefix which will be used during reporting
   :scope :string

   :counter-fn [:fn :string :nil]
   :scope-counter-fn [:fn :scope :string :nil]

   :state-fn [:fn :object :nil]
   :scope-state-fn [:fn :scope :object :nil]

   :trace-fn [:fn :string :nil]
   :scope-trace-fn [:fn :scope :string :nil]
   
   ;; called when processing fails with exception
   :error-fn [:fn :throwable :object :nil]
   :scope-error-fn [:fn :scope :throwable :object :nil]})

(defn wrap-scope
  ([context scope]
   (assoc
    context
    :counter-fn (partial (:scope-counter-fn context) (or scope "global"))
    :state-fn (partial (:scope-state-fn context) (or scope "global"))
    :trace-fn (partial (:scope-trace-fn context) (or scope "global"))
    :error-fn (partial (:scope-error-fn context) (or scope "global"))))
  ([context] (wrap-scope context nil)))

(defn create-stdout-context
  "Creates plain std out context, to be used with sample processing"
  []
  (wrap-scope
   {
    :scope-counter-fn
    (fn [scope counter]
      (println "counter increase:" scope "." counter))
    :scope-trace-fn
    (fn [scope trace]
      (println scope trace))
    :scope-state-fn
    (fn [scope state]
      (println "state set" scope state))
    :scope-error-fn
    (fn [scope throwable data]
      (println scope throwable data)
      (.printStrackTrace throwable))}))

(defn create-state-context
  "Creates state based context, adds :context-dump-fn which returns current state of
  context and :context-print-fn which outputs context state. Exception will be re thrown."
  []
  (wrap-scope
   (let [context (atom {})
         counter-fn (fn [scope counter]
                      (swap!
                       context
                       update-in [:counters scope counter]
                       (fn [value] (inc (or value 0)))))]
     {
      :scope-counter-fn counter-fn
      :scope-state-fn (fn [scope state]
                        (swap!
                         context
                         update-in [:state scope]
                         (fn [_] state)))
      :scope-trace-fn (fn [scope trace]
                        ;; todo store trace in ring buffer
                        (println scope trace)
                        )
      :scope-error-fn (fn [scope throwable data]
                        ;; todo report exception to channel in case tracing is needed
                        (counter-fn scope "exception"))
      :context-dump-fn (fn [] @context) 
      :context-print-fn (fn []
                          (let [state @context]
                            (println "state:")
                            (doseq [[scope state] (sort-by first (:state state))]
                              (println "\t" scope state))
                            (println "counters:")
                            (doseq [[scope counters] (sort-by first (:counters state))]
                              (doseq [[counter value] (sort-by first counters)]
                                (println "\t" scope counter "=" value)))))})))

(def ^:dynamic *context* (create-stdout-context))

(defn counter
  ([context counter] ((:counter-fn context) counter))
  ([counter] (counter *context* counter)) )

(def increment-counter counter)

(defn trace
  ([context trace] ((:trace-fn context) trace))
  ([trace] (trace *context* trace)))

(defn set-state
  ([context state] ((:state-fn context) state))
  ([state] (set-state *context* state)))

(defn error
  ([context throwable data] ((:error-fn context) throwable data))
  ([throwable data] (error *context* throwable data)))

(defn print-state-context [context]
  ((:context-print-fn context)))

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
