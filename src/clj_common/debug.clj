(ns clj-common.debug
  (:require
   clj-common.http-server
   clj-common.ring-middleware))

(defn print-and-return [value]
  (println value)
  value)

; taken from Joy of Clojure book

(defn readr [prompt exit-code]
  (let [input (clojure.main/repl-read prompt exit-code)]
	  (if (= input :exit)
      exit-code
	  	input)))

; (contextual-eval '{a 10} '(local-context))
(defn contextual-eval [ctx expr]
  (eval
    `(let [~@(mapcat (fn [[k v]] [k `'~v]) ctx)]
       ~expr)))

(defmacro local-context []
  (let [symbols (keys &env)]
    (zipmap
      (map
        (fn [sym] `(quote ~sym))
        symbols)
      symbols)))

; usage
; where needed put (break), when fn evaluated debug=> prompt will be shown
; all local bindings could be tested, example
; ((fn [x] (break) (+ x 2)) 10)
; use :exit to finish debugging
(defmacro break []
  `(clojure.main/repl
     :prompt #(print "debug=> ")
     :read readr
     :eval (partial contextual-eval (local-context))))

;; example url
;; http://localhost:7078/variable?namespace=a&name=variable
(defn run-debug-server
  []
  (clj-common.http-server/create-server
   7078
   (compojure.core/GET
    "/variable"
    _
    (clj-common.ring-middleware/expose-variable))))
