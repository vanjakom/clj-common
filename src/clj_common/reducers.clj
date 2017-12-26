(ns clj-common.reducers)

(require '[clojure.core.reducers :as r])

(def reduce r/reduce)
(def map r/map)
(def filter r/filter)

(defn first [reducible]
  (clojure.core/first (into [] (r/take 1 reducible))))

(defn count [reducible]
  (r/reduce
    (fn [count _] (inc count))
    0
    reducible))

(defn count-by [key-fn reducible]
  (r/reduce
    (fn [counts element]
      (let [key (key-fn element)]
        (assoc
          counts
          key
          (inc (get counts key 0)))))
    {}
    reducible))

(comment

  ; https://groups.google.com/forum/#!topic/clojure/JLXfcmHhafU
  ; Jozef Wagner

  (defn lazy-seq*
    [reducible]
    (let [c (chan)
          NIL (Object.)
          encode-nil #(if (nil? %) NIL %)
          decode-nil #(if (identical? NIL %) nil %)]
      (thread
        (reduce (fn [r v] (>!! c (encode-nil v))) nil reducible)
        (close! c))
      (take-while (complement nil?) (repeatedly #(decode-nil (<!! c))))))

  (def s (lazy-seq* (clojure.core.reducers/map inc (range))))

  (first s)

  (take 100 s)
)
