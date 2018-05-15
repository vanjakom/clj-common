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

(defn count-by-each [key-fn coll]
  (reduce
    (fn [state entry]
      (reduce
        (fn [state key]
          (update-in state [key] #(inc (or %1 0))))
        state
        (key-fn entry)))
    {}
    coll))


(comment
  (count-by-each
    :tags
    [
      {:tags #{:a :b}}
      {:tags #{:c :d}}
      {:tags #{:a}}]))

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

(defn assoc-if-value [map key value]
  (if (some? value)
    (assoc map key value)
    map))


(defn starts-upper-case [string]
  (Character/isUpperCase (.codePointAt string 0)))

; taken from
; https://stackoverflow.com/questions/9086926/create-a-proxy-for-an-specific-instance-of-an-object-in-clojure

(defmacro proxy-object
  [type delegate & body]
  (let [d (gensym)
        overrides (group-by first body)
        methods (for [m (.getMethods (resolve type))
                      :let [f (-> (.getName m)
                                symbol
                                (with-meta {:tag (-> m .getReturnType .getName)}))]
                      :when (not (overrides f))
                      :let [args (for [t (.getParameterTypes m)]
                                   (with-meta (gensym) {:tag (.getName t)}))]]
                  (list f (vec (conj args 'this))
                    `(. ~d ~f ~@(map #(with-meta % nil) args))))]
    `(let [~d ~delegate]
       (reify ~type ~@body ~@methods))))


; fn + state -> next
; fn(state) -> (next, state)
(defn fn->seq [function state]
  "Creates seq by applying function to state, returning first argument and keeping
  new state for next iteration
  example:
  (defn make-url [index] [(str \"http://example.com/\" index) (inc index)])
  (take 5 (fn->seq make-url 0))"

  (map
    first
    (drop
      1
      (iterate
        (fn [[_ state]]
          (function state))
        [nil state]))))


; taken from
; https://gist.github.com/madvas/8caab2e03e4702a8a31c
(defn partial-right
  "Takes a function f and fewer than the normal arguments to f, and
 returns a fn that takes a variable number of additional args. When
 called, the returned function calls f with additional args + args."
  ([f] f)
  ([f arg1]
   (fn [& args] (apply f (concat args [arg1]))))
  ([f arg1 arg2]
   (fn [& args] (apply f (concat args [arg1 arg2]))))
  ([f arg1 arg2 arg3]
   (fn [& args] (apply f (concat args [arg1 arg2 arg3]))))
  ([f arg1 arg2 arg3 & more]
   (fn [& args] (apply f (concat args (concat [arg1 arg2 arg3] more))))))

(defn todo [& args]
  (let [info (clojure.string/join args)]
    (throw (new
             RuntimeException
             (if (empty? info) "todo" info)))))

(defn todo-warn [& args]
  (let [info (clojure.string/join args)]
    (println (if (empty? info) "todo" info))))







