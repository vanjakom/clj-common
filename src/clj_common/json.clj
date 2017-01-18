(ns clj-common.json)

(require '[clojure.data.json :as json])

(require '[clj-common.io :as io])

(defmulti read-keyworded (fn [argument]
                           (cond
                             (instance? java.lang.String argument) :string
                             (instance? java.io.InputStream argument) :input-stream
                             (instance? java.io.Reader argument) :reader)))

(defmethod read-keyworded :string [line]
  (json/read-str line :key-fn keyword))
(defmethod read-keyworded :input-stream [stream]
  (with-open [reader (io/input-stream2reader stream)]
    (json/read reader :key-fn keyword)))
(defmethod read-keyworded :reader [reader]
  (json/read reader :key-fn keyword))

(defn read-lines-keyworded
  "Reads line by line and deserializes single line as json object.
  Note: creates lazy structure, must be consumed before stream closed."
  [input-stream]
  (let [reader (io/input-stream2reader input-stream)]
    (map
      #(read-keyworded %1)
      (line-seq (io/reader2buffered-reader reader)))))

(defn write-to-string [object]
  (json/write-str object))

(defn write-to-writer [object writer]
  (json/write object writer))

(defn write-to-stream [object output-stream]
  (with-open [writer (io/output-stream2writer output-stream)]
    (json/write object writer)))
