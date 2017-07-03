(ns clj-common.classloader)

(defn load-class-loader [jar-path]
	(java.net.URLClassLoader/newInstance
    (into-array java.net.URL [(new java.net.URL jar-path)])))

(defn load-class [class-loader class-name]
	(.loadClass class-loader class-name))

(defn get-method [clazz method-name arg-types]
  (.getMethod clazz method-name arg-types))
