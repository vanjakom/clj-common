(ns clj-common.walk)

(defn unix-timestamp-to-date [timestamp]
  (let [formatter (new java.text.SimpleDateFormat "yyyy-MM-dd")]
    (.format formatter (new java.util.Date (* timestamp 1000)))))

(defn walk-object [object schema value-type fn-on-value]
  (cond
    (map? object) (into
                    {}
                    (map
                      (fn [[key value]]
                        [key (walk-object value (key schema) value-type fn-on-value)])
                      object))
    (vector? object) (into
                       []
                       (map-indexed
                         (fn [index value]
                           (if (keyword? (first schema))
                             (walk-object value (get schema index) value-type fn-on-value)
                             (walk-object value (get schema 0) value-type fn-on-value)))
                         object))
    (seq? object) (seq
                    (into
                      []
                      (let [schema-indexed (vec schema)]
                        (map-indexed
                          (fn [index value]
                            (if (keyword? (first schema))
                              (walk-object value (get schema-indexed index) value-type fn-on-value)
                              (walk-object value (get schema-indexed 0) value-type fn-on-value)))
                          (vec object)))))
    (= schema value-type) (fn-on-value object)
    :else object))

(defn- run-tests [tests names results]
  (loop [rest-tests tests rest-names names rest-results results]
      (if (> (count rest-tests) 0)
        (if (= (first rest-results) ((first rest-tests)))
          (do
            (println "OK: " (first rest-names))
            (recur (rest rest-tests) (rest rest-names) (rest rest-results)))
          (throw (new Exception (str "FAIL: " name)))))))

(defn- walk-object-tests []
  (let [tests [
                (fn []
                  (walk-object
                    10
                    :timestamp
                    :timestamp
                    (fn [timestamp] (+ timestamp 10))))
                (fn []
                  (walk-object
                    {:a 10}
                    {:a :timestamp}
                    :timestamp
                    (fn [timestamp] (+ timestamp 10))))
                (fn []
                    (walk-object {
                                   :a 10
                                   :b {
                                        :a 10
                                        :b "value"}}
                                 {
                                   :a :timestamp
                                   :b {
                                        :a :timestamp}}
                                 :timestamp
                                 (fn [timestamp] (+ timestamp 10))))
                (fn []
                  (walk-object
                    [1 2 3]
                    [:unknown :timestamp :unknown]
                    :timestamp (fn [timestamp] (+ timestamp 10))))
                (fn []
                  (walk-object {
                                 :a 10
                                 :b {
                                      :a 10
                                      :b "value"}
                                 :c [10 "value"]}
                               {
                                 :a :timestamp
                                 :b {
                                      :a :timestamp}
                                 :c [:timestamp :value]}
                               :timestamp
                               (fn [timestamp] (+ timestamp 10))))
                (fn []
                  (walk-object
                    '(1 2 3)
                    '(:unknown :timestamp :unknown)
                    :timestamp
                    (fn [timestamp] (+ timestamp 10))))
                (fn []
                  (walk-object
                    [[1 2] [2 3] [3 4]]
                    [[:timestamp :unknown]]
                    :timestamp
                    (fn [timestamp] (+ timestamp 10))))
                (fn []
                  (walk-object
                    (list [1 2] [2 3] [3 4])
                    [[:timestamp :unknown]]
                    :timestamp
                    (fn [timestamp] (+ timestamp 10))))]
        names [
                "single entry"
                "simple map"
                "complex map"
                "simple vector"
                "map and vector"
                "simple list"
                "vector of defined vectors"
                "list of defined vectors"]
        results [
                  20
                  {:a 20}
                  {:a 20, :b {:a 20, :b "value"}}
                  [1 12 3]
                  {:a 20, :b {:a 20, :b "value"}, :c [20 "value"]}
                  '(1 12 3)
                  [[11 2] [12 3] [13 4]]
                  '([11 2] [12 3] [13 4])]]
    (run-tests tests names results)))
