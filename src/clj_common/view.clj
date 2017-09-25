(ns clj-common.view)

(defn seq->map [key-gen-fn sequence]
  (into
    {}
    (map
      (fn [element] [(key-gen-fn element) element])
      sequence)))

(defn seq->set
  ([set-or-seq]
  (if (set? set-or-seq)
    set-or-seq
    (into #{} set-or-seq)))
  ([key-fn sequence]
   (into #{} (map key-fn sequence))))
