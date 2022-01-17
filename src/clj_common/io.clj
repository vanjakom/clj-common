(ns clj-common.io
  (:import
    java.io.ByteArrayOutputStream
    java.io.ByteArrayInputStream
    java.nio.ByteBuffer ))

; note
; clj-common.as should replace most of transformers in clj-common.io

(defn bytes->input-stream [byte-array]
  (new java.io.ByteArrayInputStream byte-array))
(def byte2input-stream bytes->input-stream)

(defn byte-output-stream->bytes [byte-output-stream]
  (.toByteArray byte-output-stream))
(defn create-byte-output-stream []
  (new ByteArrayOutputStream))

(defn string->input-stream [str-value]
  (new java.io.ByteArrayInputStream (.getBytes str-value)))

; https://stackoverflow.com/questions/1252468/java-converting-string-to-and-from-bytebuffer-and-associated-problems
(defn string->byte-buffer [string]
  (ByteBuffer/wrap (.getBytes string)))

; https://stackoverflow.com/questions/1252468/java-converting-string-to-and-from-bytebuffer-and-associated-problems
(defn byte-buffer->string [byte-buffer]
  (new
    String
    (if (.hasArray byte-buffer)
      (.array byte-buffer)
      (let [bytes (byte-array (.remaining byte-buffer))]
        (.get byte-buffer bytes)
        bytes))))

(def string2input-stream string->input-stream)

(defn string2reader [str-value]
  (new java.io.InputStreamReader (string2input-stream str-value)))

(defn string2buffered-reader [str-value]
  (new java.io.BufferedReader (string2reader str-value)))

(defn input-stream2reader [input-stream]
  (new java.io.InputStreamReader input-stream))

(defn input-stream->buffered-reader [input-stream]
  (new java.io.BufferedReader
       (new java.io.InputStreamReader input-stream "UTF-8")))

(def input-stream2buffered-reader input-stream->buffered-reader)

(defn input-stream->pushback-reader [input-stream]
  (new
    java.io.PushbackReader
    (new
      java.io.InputStreamReader input-stream)))

(defn output-stream->writer [output-stream]
  (new java.io.OutputStreamWriter output-stream "UTF-8"))
(def output-stream2writer output-stream->writer)

(defn output-stream2buffered-writer [output-stream]
  (new
    java.io.BufferedWriter
    (new java.io.OutputStreamWriter output-stream)))

(defn input-stream->bytes [input-stream]
  (org.apache.commons.io.IOUtils/toByteArray input-stream))

(def input-stream->byte-array input-stream->bytes)
(def input-stream2bytes input-stream->bytes)

(defn input-stream->string [input-stream]
  (org.apache.commons.io.IOUtils/toString input-stream))

(def input-stream2string input-stream->string)

(defn reader2buffered-reader [reader]
  (new java.io.BufferedReader reader))

(defn copy-input-to-output-stream [input-stream output-stream]
  (clojure.java.io/copy input-stream output-stream))

(defn url2input-stream [url-string]
  (let [url (new java.net.URL url-string)]
    (.openStream url)))

(defn input-stream->line-seq
  [input-stream]
  ;; maybe to check if input stream is already reader
  ;; return empty seq on empty input stream, makes difference when using
  ;; http to distinguish empty 200 from 404 
  (or
   (line-seq (input-stream->buffered-reader input-stream))
   '()))

(defn input-stream->std-out
  [input-stream]
  (doseq [line (input-stream->line-seq input-stream)]
    (println line)))

(defn write [output-stream bytes]
  (.write output-stream bytes))

(defn write-string [output-stream string]
  (.write output-stream (.getBytes string "utf-8")))

(defn write-line [output-stream line]
  (.write output-stream (.getBytes line))
  (.write output-stream (.getBytes "\n")))

(defn write-new-line [output-stream]
  (.write output-stream (.getBytes "\n")))

(defn read-line [buffered-reader]
  (.readLine buffered-reader))

;; todo bug, parsing of json streamed through seq->input-stream served from compojure
;; fails, seems that best would be to create java util and put this code inside
#_(defn streamable->input-stream [coll]
  (let [next-streamable (first coll)
        rest-coll (rest coll)]
    (cond
      (nil? next-streamable)
        (streamable->input-stream rest-coll)
      (instance? java.lang.String next-streamable)
        [(string->input-stream next-streamable) rest-coll]
      (instance? java.util.concurrent.Future next-streamable)
        (streamable->input-stream (concat [(.get next-streamable)] rest-coll))
      (instance? java.io.InputStream next-streamable)
        [next-streamable rest-coll]
      (instance? clojure.lang.Seqable next-streamable)
        (streamable->input-stream
          (concat
            next-streamable rest-coll))
      (instance? java.lang.Object next-streamable)
        [(string->input-stream (str next-streamable)) rest-coll]
      :else nil)))
#_(defn input-stream-proxy [read-fn]
  (let [read-in-array-fn (fn [^bytes array off len]
                           (if (nil? array)
                             (throw (new NullPointerException)))
                           (if (or (< off 0) (< len 0) (> len (- (alength array) off)))
                             (throw (new IndexOutOfBoundsException)))
                           (if (= len 0)
                             0
                             (do
                               (let [c (read-fn)]
                                 (if (= c -1)
                                   -1
                                   (do
                                     (aset-byte array (int off) (unchecked-byte c))
                                     (loop [i 1
                                            c (read-fn)]
                                       (if (= c -1)
                                         i
                                         (do
                                           (aset-byte array (int (+ off i)) (unchecked-byte c))
                                           (if (< (inc i) len)
                                             (recur (inc i) (read-fn))
                                             i))))))))))]
    (proxy [java.io.InputStream] []
      (read ([] (read-fn))
            ([^bytes bytes] (read-in-array-fn bytes 0 (alength bytes)))
            ([^bytes bytes off len] (read-in-array-fn bytes off len))))))
#_(defn seq->input-stream [coll]
  (let [state (volatile! (streamable->input-stream coll))]
    (input-stream-proxy
      (fn read-once []
        (let [[current-stream coll] @state]
          (if (some? current-stream)
            (let [next-byte (.read current-stream)]
              (if
                (= next-byte -1)
                (if (empty? coll)
                  -1
                  (do
                    (vreset!
                      state
                      (streamable->input-stream coll))
                    (read-once)))
                next-byte))
            -1))))))

#_(do
  (input-stream->string
    (seq->input-stream [
                         "test1"
                         (new java.io.ByteArrayInputStream (.getBytes "test2"))
                         "abc"]))

  (input-stream->string
    (let [original (new java.io.ByteArrayInputStream (.getBytes "test"))]
      (input-stream-proxy
        (fn []
          (.read original)))))
)

; cache input stream
; https://stackoverflow.com/questions/5923817/how-to-clone-an-inputstream
(defn cache-input-stream
  "Returns fn which on invocation will return InputStream which would produce
  same data as original one"
  [input-stream]
  (let [output-stream (new java.io.ByteArrayOutputStream)]
    (copy-input-to-output-stream input-stream output-stream)
    (let [bytes (.toByteArray output-stream)]
      (fn []
        (new java.io.ByteArrayInputStream bytes)))))

(defn create-buffer-output-stream []
  (new ByteArrayOutputStream))
(def buffer-output-stream create-buffer-output-stream)

(defn buffer-output-stream->input-stream [buffer-output-stream]
  (let [byte-array (.toByteArray buffer-output-stream)]
    (new ByteArrayInputStream byte-array)))

(defn decompress-gz [input-stream]
  (new
   java.util.zip.GZIPInputStream
   input-stream))

(defn create-string-writer []
  (new java.io.StringWriter))

(defn string-writer->string [string-writer]
  (.toString string-writer))
