(ns clj-common.io)

(defn string2input-stream [str-value]
  (new java.io.ByteArrayInputStream (.getBytes str-value)))

(defn string2reader [str-value]
  (new java.io.InputStreamReader (string2input-stream str-value)))

(defn string2buffered-reader [str-value]
  (new java.io.BufferedReader (string2reader str-value)))

(defn input-stream2reader [input-stream]
  (new java.io.InputStreamReader input-stream))

(defn output-stream2writer [output-stream]
  (new java.io.OutputStreamWriter output-stream))

(defn input-stream2bytes [input-stream]
  (org.apache.commons.io.IOUtils/toByteArray input-stream))

(defn reader2buffered-reader [reader]
  (new java.io.BufferedReader reader))

(defn copy-input-to-output-stream [input-stream output-stream]
  (clojure.java.io/copy input-stream output-stream))
