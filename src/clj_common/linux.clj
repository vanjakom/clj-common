(ns clj-common.linux
  (:require [clj-common.as :as as]))


(defn ls-line->map [ls-line]
  (let [ls-line-formatted (.replaceAll ls-line "[\\s]+" "\t")
        splits (clojure.string/split ls-line-formatted #"\t")]
    {
     :size (as/as-long (get splits 4))}))

(defn ls->size [ls-line]
  (:size (ls-line->map ls-line)))


(defn aws-ls->size [aws-ls-line]
  (let [splits (clojure.string/split aws-ls-line #" ")]
    (as/as-long (get splits 2))))

(defn aws-ls-prefix->name [aws-ls-prefix-line]
  (let [ls-line-formatted (.replaceAll aws-ls-prefix-line "[\\s]+" "\t")
        splits (clojure.string/split ls-line-formatted #"\t")]
    ; (count splits)
    ; (doseq [split splits]
    ;   (println "\t" split))
    (let [name-with-last-slash (get splits 2)]
      (.substring name-with-last-slash 0 (dec (count name-with-last-slash))))))
