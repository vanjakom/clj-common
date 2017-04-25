(ns clj-common.clojure)

(require '[clj-common.exception :as exception])

; from early days :)
(defn not-nil? [value]
  (some? value))

(defn not-empty? [value]
  (not (empty? value)))

; http://stackoverflow.com/questions/11676120/why-dont-when-let-and-if-let-support-multiple-bindings-by-default
(defmacro if-let*
  ([bindings then]
   `(if-let* ~bindings ~then nil))
  ([bindings then else]
   (if (seq bindings)
     `(if-let [~(first bindings) ~(second bindings)]
        (if-let* ~(drop 2 bindings) ~then ~else)
        ~(if-not (second bindings) else))
     then)))

(defn flatten-one-level [coll]
  (apply
    concat
    coll))

(defmacro timed-fn [name & exprs]
  `(let [start# (System/nanoTime)]
    (let [result# ~@exprs
          duration# (/ (- (System/nanoTime) start#) 1000000.0)]
      (println ~name " duration: " duration#)
      result#)))



(defn print-return [value]
  (println value)
  value)

(defmacro multiple-do
  "example:
  (multiple-do
	              [
              		:original identity
              		:inc inc :plus-three (partial + 3)]
              	5)"
  [bindings value]
  `(apply
     array-map
     (flatten
       (map
         (fn [[key# func#]]
           [key# (func# ~value)])
         (partition
           2
           ~bindings)))))

;(defmacro multiple-reduce
;  "example:
;  (multiple-reduce [
;                     :inc 0 #(+ %1 (inc %2))
;                     :dec 0 #(+ %1 (dec %2))]
;                   '(1 2 3))"
;  [bindings coll]
;  `(reduce
;     (fn [state# value#]
;       (exception/with-data {:state state# :value value#}
;         (apply
;           array-map
;           (flatten-one-level
;             (map
;               (fn [[key# default# func#]]
;                 [key# (func# (get state# key# default#) value#)])
;               (partition
;                 3
;                 ~bindings))))))
;     {}
;     ~coll))

(defn multiple-reduce
  "example:
  (multiple-reduce [
                     :inc 0 #(+ %1 (inc %2))
                     :dec 0 #(+ %1 (dec %2))]
                   '(1 2 3))"
  [bindings coll]
  (reduce
    (fn [state value]
      (exception/with-data {:state state :value value}
        (apply
          array-map
          (flatten-one-level
            (map
              (fn [[key start func]]
                [key (func (get state key start) value)])
              (partition
                3
                bindings))))))
    {}
    coll))



(defn starts-upper-case [string]
  (Character/isUpperCase (.codePointAt string 0)))

