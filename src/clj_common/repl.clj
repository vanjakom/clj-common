(ns clj-common.repl)

(require '[clj-common.path :as path])
(require '[clj-common.io :as io])
(require '[clj-common.localfs :as fs])
(require '[clj-common.jvm :as jvm])

(require 'clojure.tools.nrepl.server)
(require 'lighttable.nrepl.handler)

(require 'clojure.pprint)

(defn is-static [field-or-method]
  (java.lang.reflect.Modifier/isStatic (.getModifiers field-or-method)))

(defn analyze-field [field]
  {
    :name (.getName field)
    :type (.getType field)})

(defn analyze-method [method]
  {
    :name (.getName method)
    :return (.getReturnType method)
    :arguments (.getParameterTypes method)})

(defn analyze-class [clazz]
  (let [fields (.getFields clazz)
        instance-fields (filter #(not (is-static %1)) fields)
        static-fields (filter is-static fields)
        methods (.getMethods clazz)
        instance-methods (filter #(not (is-static %1)) methods)
        static-methods (filter is-static methods)]
    {
      :static-fields (map analyze-field static-fields)
      :fields (map analyze-field instance-fields)
      :methods (map analyze-method instance-methods)
      :static-methods (map analyze-method static-methods)}))

(defn print-class [clazz]
  (clojure.pprint/pprint (analyze-class clazz)))

(defn print-seq [seq]
  (doseq [elem seq]
    (println elem)))


(defmacro fn-with-source [& definition]
  (println "form: " &form)
  (println "definition: " definition)
  `(with-meta
     (clojure.core/fn ~@definition)
     {
       :source (quote ~&form)}))

(defmacro defn-with-source [fn-name & definition]
  (println "form: " &form)
  (println "name: " fn-name)
  (println "class: " (class fn-name))
  (println "definition: " definition)
  (let [fn-name-str (name fn-name)]
    `(with-meta
       (clojure.core/fn ~@definition)
       {
         :source (quote ~&form)
         :namespace (.getName *ns*)
         :name ~fn-name-str})))

(defn nrepl-port []
  ; until better solution is found
  (let [port-file-path (path/child
                         (jvm/jvm-path)
                         ".nrepl-port")]
    (if (fs/exists? port-file-path)
      (Long/parseLong
        (io/input-stream->string
          (fs/input-stream port-file-path))))))

; useful
; https://groups.google.com/forum/#!topic/light-table-discussion/PPUtWjV9iY4
(defn start-lighttable-nrepl-server [port]
  (clojure.tools.nrepl.server/start-server
    :port port
    :handler (clojure.tools.nrepl.server/default-handler #'lighttable.nrepl.handler/lighttable-ops)))

(defn eval-on-nrepl-server [nrepl-port string-to-eval]
  (with-open [conn (clojure.tools.nrepl/connect :port nrepl-port)]
   (->
      (clojure.tools.nrepl/client conn 1000)
      (clojure.tools.nrepl/message {:op "eval" :code string-to-eval})
      clojure.tools.nrepl/response-values)))
