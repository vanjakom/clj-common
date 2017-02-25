(ns clj-common.view)

(defn set-from-seq [key-gen-fn data-seq]
  (into
    #{}
    (map
      key-gen-fn
      data-seq)))

(defn map-from-seq [key-gen-fn data-seq]
  (into
    {}
    (map
      (fn [data] [(key-gen-fn data) data])
      data-seq)))
