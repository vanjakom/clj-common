(ns clj-common.edn)

(defn write-object [output-stream object]
  (.write output-stream (.getBytes (pr-str object)))
  (.write output-stream (.getBytes "\n")))

