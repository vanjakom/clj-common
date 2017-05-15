(ns clj-common.repl)

(require '[clj-common.path :as path])
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

(defn jvm-local-path
  "Returns path from which JVM is started"
  []
  (path/path4string (System/getProperty "user.dir")))

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

