(ns clj-common.exception)

(defmacro with-data
  "example:
  (let [state {:value 1}]
    (do state
      (max (:value state) (:max state))))"
  [data & expressions]
  `(try
     ~@expressions
     (catch Exception e# (throw (ex-info (.getMessage e#) ~data e#)))))
