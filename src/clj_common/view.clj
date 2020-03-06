(ns clj-common.view)

(defn seq->map
  ([key-gen-fn sequence]
   (into
    {}
    (map
      (fn [element] [(key-gen-fn element) element])
      sequence)))
  ([key-gen-fn value-gen-fn sequence]
   (into
    {}
    (map
      (fn [element] [(key-gen-fn element) (value-gen-fn element)])
      sequence))))

(defn seq->set
  ([set-or-seq]
  (if (set? set-or-seq)
    set-or-seq
    (into #{} set-or-seq)))
  ; uses key-fn to pick latest occurance of element
  ([key-fn sequence]
   (into #{} (vals (seq->map key-fn sequence)))))

(comment
  (= #{1 2 3 4} (seq->set [1 2 3 1 2 4]))

  (=
    #{{:name :b :value 2} {:name :a :value 2}}
    (seq->set
      :name
      [{:name :a :value 1} {:name :a :value 2} {:name :b :value 2}]))

)
