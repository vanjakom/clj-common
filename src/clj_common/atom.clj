(ns clj-common.atom)

(defn get-and-set! [atm newv]
  (loop [oldv @atm]
    (if (compare-and-set! atm oldv newv)
      oldv
    (recur @atm))))

(defn queue-push! [list-atom value]
  (swap!
    list-atom
    (fn [state]
      (conj state value))))

(defn queue-poll! [list-atom]
  (loop [queue @list-atom]
    (let [value (last queue)]
      (if
        (compare-and-set! list-atom queue (drop-last queue))
        value
        (recur @list-atom)))))
