(ns clj-common.test
  (:require [clj-common.logging :as logging]))

; something similar to clojure.core/test except test definitions are following defn
; https://clojuredocs.org/clojure.core/test


(defmacro test [test-name & expressions]
  `(if
     (try
       (logging/report {
                         :fn 'clj-common.test/test
                         :test ~test-name})
       (not (and ~@expressions))
       (catch Exception e#
         (throw (new RuntimeException (str "Test failed with exception: " ~test-name) e#))))
     (throw (new RuntimeException (str "Test failed: " ~test-name)))))



(test
  "testing true"
  true)

(test
  "testing true"
  (= 1 1)
  (= "string" "string"))

(test
  (try
    (test
      "testing false"
      false)
    false
    (catch Exception e true)))
