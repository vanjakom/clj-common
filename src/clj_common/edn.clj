(ns clj-common.edn)

(require '[clj-common.io :as io])
(require '[clojure.edn :as edn])

(defn write-object [output-stream object]
  (.write output-stream (.getBytes (pr-str object)))
  (.write output-stream (.getBytes "\n")))

(defn read-object [input-stream]
  (edn/read (io/input-stream->pushback-reader input-stream)))
