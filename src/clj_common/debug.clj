(ns clj-common.debug
  (:require
   clojure.pprint
   clj-common.http-server
   clj-common.ring-middleware
   clj-common.io))

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
   (compojure.core/routes
    (compojure.core/ANY
     "/echo"
     _
     (fn [request]
       (clojure.pprint/pprint request)
       (run!
        println
        (clj-common.io/input-stream->line-seq (:body request)))
       {:status 200}))
    
    (compojure.core/GET
     "/variable"
     _
     (clj-common.ring-middleware/expose-variable))
    (compojure.core/GET
     "/plot"
     _
     (clj-common.ring-middleware/expose-plot))
    (compojure.core/GET
     "/time-plot"
     _
     (clj-common.ring-middleware/expose-timeseries-plot)))))

(run-debug-server)

