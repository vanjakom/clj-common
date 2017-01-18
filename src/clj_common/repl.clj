(ns clj-common.repl)

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
