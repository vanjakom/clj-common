(ns clj-common.json)

(require '[clojure.data.json :as json])

;; note issues with unicode characters
;; https://clojure.github.io/data.json/#clojure.data.json/write
;; use :escape-unicode false

(require '[clj-common.io :as io])

(defmulti read-keyworded
  (fn [argument]
    (cond
      (instance? java.lang.String argument) :string
      (instance? (Class/forName "[B") argument) :bytes
      (instance? java.io.InputStream argument) :input-stream
      (instance? java.io.Reader argument) :reader)))

(defmethod read-keyworded :string [line]
  (json/read-str line :key-fn keyword))
(defmethod read-keyworded :bytes [bytes]
  (with-open [reader (io/input-stream2reader
                       (io/bytes->input-stream bytes))]
    (json/read reader :key-fn keyword)))
(defmethod read-keyworded :input-stream [stream]
  (with-open [reader (io/input-stream2reader stream)]
    (json/read reader :key-fn keyword)))
(defmethod read-keyworded :reader [reader]
  (json/read reader :key-fn keyword))

(defmulti read
  (fn [argument]
    (cond
      (instance? java.lang.String argument) :string
      (instance? (Class/forName "[B") argument) :bytes
      (instance? java.io.InputStream argument) :input-stream
      (instance? java.io.Reader argument) :reader)))

(defmethod read :string [line]
  (json/read-str line))
(defmethod read :bytes [bytes]
  (with-open [reader (io/input-stream2reader
                       (io/bytes->input-stream bytes))]
    (json/read reader)))
(defmethod read :input-stream [stream]
  (with-open [reader (io/input-stream2reader stream)]
    (json/read reader)))
(defmethod read :reader [reader]
  (json/read reader))

(defn read-lines-keyworded
  "Reads line by line and deserializes single line as json object.
  Note: creates lazy structure, must be consumed before stream closed."
  [input-stream]
  (let [reader (io/input-stream2reader input-stream)]
    (map
      #(read-keyworded %1)
      (line-seq (io/reader2buffered-reader reader)))))

(defn read-lines
  "Reads line by line and deserializes single line as json object.
  Note: creates lazy structure, must be consumed before stream closed."
  [input-stream]
  (let [reader (io/input-stream2reader input-stream)]
    (map
      #(clj-common.json/read %1)
      (line-seq (io/reader2buffered-reader reader)))))

(defn write-to-string [object]
  (json/write-str object :escape-unicode false))

(defn write-lines-to-string [object-seq]
  (clojure.string/join
    "\n"
    (map
      #(json/write-str % :escape-unicode false)
      object-seq)))

(defn write-to-writer [object writer]
  (json/write object writer :escape-unicode false))

(defn write-to-stream [object output-stream]
  (let [writer (io/output-stream2writer output-stream)]
    (json/write object writer :escape-unicode false)
    (.flush writer)))

(defn write-pretty-print [object writer]
  (binding [*out* writer
            *print-length* Long/MAX_VALUE]
    (json/pprint object :escape-unicode false)
    (.flush writer)))

(defn write-to-line-stream [object output-stream]
  (let [writer (io/output-stream2writer output-stream)]
    (json/write object writer :escape-unicode false)
    (.write writer 10) ; new line char
    (.flush writer)))

(defn serialize [object]
  (let [byte-output-stream (io/create-byte-output-stream)]
    (write-to-stream object byte-output-stream)
    (io/byte-output-stream->bytes byte-output-stream)))
(def json->bytes serialize)

(defn deserialize [bytes]
  (let [input-stream (io/bytes->input-stream bytes)]
    (read-keyworded input-stream)))
(def bytes->json deserialize)




