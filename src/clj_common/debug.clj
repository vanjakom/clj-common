(ns clj-common.debug)

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
