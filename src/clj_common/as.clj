(ns clj-common.as)

;; idea
; ensures type
; per each type two methods must exist nil and unknown

; usage
; (require '[clj-common.as :as as])
; (.java-method object (as/integer value))

; todo remove (var dispatch-fn) wrapper once everything is working
; todo dispatch for all numbers could be same? ...

; integer

(defn as-integer-dispatch [value]
  (cond
    (nil? value) :nil
    (instance? Number value) :number
    (instance? String value) :string
    :else :unknown))

(defmulti as-integer as-integer-dispatch)

(defmethod as-integer :number [^Number value]
  (.intValue value))

(defmethod as-integer :string [value]
  (Integer/parseInt value))

(def ^:dynamic *default-integer* (int 0))
(defmethod as-integer :nil [_]
  *default-integer*)

(defmethod as-integer :unknown [value]
  (throw (new RuntimeException (str "No transformer to Integer from " (class value)))))


;; long

(defn as-long-dispatch [value]
  (cond
    (nil? value) :nil
    (instance? Number value) :number
    (instance? String value) :string
    :else :unknown))

(defmulti as-long as-long-dispatch)

(defmethod as-long :number [^Number value]
  (.longValue value))

(defmethod as-long :string [value]
  (Long/parseLong value))

(def ^:dynamic *default-long* (long 0))
(defmethod as-long :nil [_]
  *default-long*)

(defmethod as-long :unknown [value]
  (throw (new RuntimeException (str "No transformer to Long from " (class value)))))



;; double

(defn as-double-dispatch [value]
  (cond
    (nil? value) :nil
    (instance? Number value) :number
    (instance? String value) :string
    :else :unknown))

(defmulti as-double as-double-dispatch)

(defmethod as-double :number [^Number value]
  (.doubleValue value))

(defmethod as-double :string [value]
  (Double/parseDouble value))

(def ^:dynamic *default-double* (clojure.core/double 0))
(defmethod as-double :nil [_]
  *default-double*)

(defmethod as-double :unknown [value]
  (throw (new RuntimeException (str "No transformer to Double from " (class value)))))


;; keyword

(defn as-keyword-dispatch [value]
  (cond
    (nil? value) :nil
    (keyword? value) :keyword
    (instance? String value) :string
    :else :unknown))

(defmulti as-keyword as-keyword-dispatch)

(def ^:dynamic *default-keyword* nil)
(defmethod as-keyword :nil [_]
  *default-keyword*)

(defmethod as-keyword :keyword [value]
  value)

(defmethod as-keyword :string [value]
  (clojure.core/keyword value))

;; string

(defn as-string-dispatch [value]
  (cond
    (nil? value) :nil
    (keyword? value) :keyword
    (instance? String value) :string
    :else :unknown))

(defmulti as-string as-string-dispatch)

(defmethod as-string :nil [_] nil)

(defmethod as-string :keyword [value]
  (name value))

(defmethod as-string :string [value]
  value)

(defmethod as-string :unknown [value]
  (.toString value))

#_(do
  (as-integer "10")
  (as-integer nil)
  (as-integer 15.1)
  (as-integer {:a 10})

  (class (double (as-integer "10"))))
