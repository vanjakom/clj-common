(ns clj-common.as)

; idea
; ensures type
; per each type two methods must exist nil and unknown

; usage
; (require '[clj-common.as :as as])
; (.java-method object (as/integer value))

; todo remove (var dispatch-fn) wrapper once everything is working
; todo dispatch for all numbers could be same? ...

; integer

(defn integer-dispatch [value]
  (cond
    (nil? value) :nil
    (instance? Number value) :number
    (instance? String value) :string
    :else :unknown))

(defmulti integer (var integer-dispatch))

(defmethod integer :number [^Number value]
  (.intValue value))

(defmethod integer :string [value]
  (Integer/parseInt value))

(def ^:dynamic *default-integer* (int 0))
(defmethod integer :nil [_]
  *default-integer*)

(defmethod integer :unknown [value]
  (throw (new RuntimeException (str "No transformer to Integer from " (class value)))))

; double

(defn double-dispatch [value]
  (cond
    (nil? value) :nil
    (instance? Number value) :number
    (instance? String value) :string
    :else :unknown))

(defmulti double (var double-dispatch))

(defmethod double :number [^Number value]
  (.doubleValue value))

(defmethod double :string [value]
  (Double/parseDouble value))

(def ^:dynamic *default-double* (clojure.core/double 0))
(defmethod double :nil [_]
  *default-double*)

(defmethod double :unknown [value]
  (throw (new RuntimeException (str "No transformer to Double from " (class value)))))


; keyword

(defn keyword-dispatch [value]
  (cond
    (nil? value) :nil
    (keyword? value) :keyword
    (instance? String value) :string
    :else :unknown))

(defmulti keyword (var keyword-dispatch))

(def ^:dynamic *default-keyword* nil)
(defmethod keyword :nil [_]
  *default-keyword*)

(defmethod keyword :keyword [value]
  value)

(defmethod keyword :string [value]
  (clojure.core/keyword value))

(comment
  (integer "10")
  (integer nil)
  (integer 15.1)
  (integer {:a 10})

  (class (double (integer "10"))))


