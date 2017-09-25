(ns clj-common.io)

(defn byte2input-stream [byte-array]
  (new java.io.ByteArrayInputStream byte-array))

(defn string->input-stream [str-value]
  (new java.io.ByteArrayInputStream (.getBytes str-value)))

(def string2input-stream string->input-stream)

(defn string2reader [str-value]
  (new java.io.InputStreamReader (string2input-stream str-value)))

(defn string2buffered-reader [str-value]
  (new java.io.BufferedReader (string2reader str-value)))

(defn input-stream2reader [input-stream]
  (new java.io.InputStreamReader input-stream))

(defn input-stream->buffered-reader [input-stream]
  (new java.io.BufferedReader
       (new java.io.InputStreamReader input-stream)))

(def input-stream2buffered-reader input-stream->buffered-reader)

(defn output-stream2writer [output-stream]
  (new java.io.OutputStreamWriter output-stream))

(defn output-stream2buffered-writer [output-stream]
  (new
    java.io.BufferedWriter
    (new java.io.OutputStreamWriter output-stream)))

(defn input-stream->byte-array [input-stream]
  (org.apache.commons.io.IOUtils/toByteArray input-stream))

(def input-stream2bytes input-stream->byte-array)

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

(defn write [output-stream bytes]
  (.write output-stream bytes))

(defn write-string [output-stream string]
  (.write output-stream (.getBytes string)))

(defn write-line [output-stream line]
  (.write output-stream (.getBytes line))
  (.write output-stream (.getBytes "\n")))

(defn read-line [buffered-reader]
  (.readLine buffered-reader))

