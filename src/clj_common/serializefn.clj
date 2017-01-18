(ns clj-common.serializefn)

(def ^:private context (list))

(clojure.core/defn add-to-context [name definition]
  (alter-var-root
    (var context)
    (fn [_]
      (conj context definition))))

(clojure.core/defn reset-context []
  (alter-var-root
    (var context)
    (fn [_]
      (list))))

(clojure.core/defn export-context []
  (first
    (reduce
      (fn [[entries entry-set] entry]
        (let [entry-id (str (:namespace entry) "/" (:name entry))]
          (if
            (not (contains? entry-set entry-id))
            [(conj entries entry) (conj entry-set entry-id)]
            [entries entry-set])))
      [(list) #{}]
      context)))

(defmacro defn [fn-name & definition]
  (let [fn-name-str (name fn-name)
        namespace-str (ns-name *ns*)
        source (conj (rest &form) 'fn)]
    (add-to-context
      (str namespace-str "/" fn-name-str)
      {
        :name fn-name-str
        :namespace namespace-str
        :source (pr-str source)}))
  `(clojure.core/defn ~fn-name ~@definition))

(clojure.core/defn create-or-return-ns [namespace-str]
  (println "checking " namespace-str)
  (let [existing-ns (find-ns (symbol namespace-str))]
    (if existing-ns
      (do
        (println "exists " namespace-str)
        existing-ns)
      (do
        (println "creating " namespace-str)
        (let [new-ns (create-ns (symbol namespace-str))]
          (binding [*ns* new-ns]
            (clojure.core/refer 'clojure.core))
          new-ns)))))

(clojure.core/defn restore-context [context]
  (println "restoring context")
  (doseq [entry context]
    (let [namespace (create-or-return-ns (:namespace entry))]
      (binding [*ns* namespace]
        (println "current ns: " (ns-name *ns*))
        (println "defining " (:name entry) " in " (:namespace entry))
        (intern
          namespace
          (symbol (:name entry))
          (eval (read-string (:source entry))))))))

