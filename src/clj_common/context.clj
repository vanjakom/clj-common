(ns clj-common.context
  (:use clj-common.clojure))

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
   ;; make scope defining more pleasant for eyes, it should be either:
   ;; ["general"] or ["scope1" "scope2" ... "scopen"]
   (let [parent-scope (:scope context)
         scope (conj
                (or parent-scope ["general"])
                (or scope "general"))
         scope (if (and (> (count scope) 1) (= (first scope) "general"))
                 (into [] (rest scope))
                 scope)]
     (assoc
      context
      :scope scope
      :counter-fn (partial (:scope-counter-fn context) scope)
      :state-fn (partial (:scope-state-fn context) scope)
      :trace-fn (partial (:scope-trace-fn context) scope)
      :error-fn (partial (:scope-error-fn context) scope))))
  ([context] (wrap-scope context nil)))

(defn create-stdout-context
  "Creates plain std out context, to be used with sample processing"
  []
  (wrap-scope
   {
    :scope-counter-fn
    (fn [scope counter] (report "counter increase:" (clojure.string/join "." scope) "." counter))
    :scope-trace-fn
    (fn [scope trace] (report (clojure.string/join "." scope) trace))
    :scope-state-fn
    (fn [scope state] (report "state set" (clojure.string/join "." scope) state))
    :scope-error-fn
    (fn [scope throwable data]
      (report (clojure.string/join "." scope) throwable data)
      ;; todo
      (.printStrackTrace throwable))}))

(defn create-state-context
  "Creates state based context, adds :context-dump-fn which returns current
  state of context and :context-print-fn which outputs context state. Exception
  will be re thrown."
  []
  (wrap-scope
   (let [context (atom {})
         counter-fn (fn [scope counter]
                      (swap!
                       context
                       update-in [:counters (clojure.string/join "." scope) counter]
                       (fn [value] (inc (or value 0)))))]
     {
      :scope-counter-fn counter-fn
      :scope-state-fn (fn [scope state]
                        (swap!
                         context
                         update-in [:state (clojure.string/join "." scope)]
                         (constantly state)))
      :scope-trace-fn (fn [scope trace]
                        ;; todo store trace in ring buffer
                        (report (clojure.string/join "." scope) trace))
      :scope-error-fn (fn [scope throwable data]
                        (let [output (str
                                      (throwable->string throwable)
                                      (if-let [data data]
                                        (str "Data:\n" data)
                                        "No data"))]
                          (report output)
                          (counter-fn (clojure.string/join "." scope) "exception")))
      :context-dump-fn (fn [] @context) 
      :context-print-fn (fn []
                          (let [state @context]
                            (report "state:")
                            (doseq [[scope-str state] (sort-by first (:state state))]
                              (report "\t" scope-str state))
                            (report "counters:")
                            (doseq [[scope-str counters]
                                    (sort-by first (:counters state))]
                              (doseq [[counter value] (sort-by first counters)]
                                (report "\t" scope-str counter "=" value)))))})))

(def ^:dynamic *context* (create-stdout-context))

(defn counter
  ([context counter] ((:counter-fn context) counter))
  ([counter] ((:counter-fn  *context*) counter)) )

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
