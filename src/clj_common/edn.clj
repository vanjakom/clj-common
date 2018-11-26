(ns clj-common.edn
  (:require
   [clj-common.io :as io]
   [clojure.edn :as edn]))

(defn write-object
  "Writes given object serialized either to output stream or string depending on
  arity called"
  ([output-stream object]
   ;; truncates seq to 100 elements otherwise
   (binding [*print-length* nil]
     (.write
      output-stream
      (.getBytes (prn-str object)))))
  ([object]
   (binding [*print-length* nil]
     (prn-str object))))

(defn read-object [input-stream]
  (edn/read (io/input-stream->pushback-reader input-stream)))

(defmulti read
  (fn [argument]
    (cond
      (instance? java.lang.String argument) :string
      (instance? java.io.InputStream argument) :input-stream)))

(defmethod read :string [line]
  (try
    (edn/read-string line)
    (catch Exception e (throw (ex-info "unable to parse edn" {:line line} e)))))

(defmethod read :input-stream [input-stream]
  (edn/read (io/input-stream->pushback-reader input-stream)))

(defn input-stream->seq
  "Transforms line EDN input stream to seq of objects"
  [input-stream]
  (map read (io/input-stream->line-seq input-stream)))

