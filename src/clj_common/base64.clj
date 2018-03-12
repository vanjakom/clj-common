(ns clj-common.base64)

(require '[clj-common.io :as io])

(defn base64->bytes [^String base64-as-string]
  (org.apache.commons.codec.binary.Base64/decodeBase64 (.getBytes base64-as-string)))
(def base64->byte-array base64->bytes)


(defn base64->string [^String base64-as-string]
  (new String (base64->byte-array base64-as-string)))

(defn string->base64-string [^String string]
  (new
    String
    (org.apache.commons.codec.binary.Base64/encodeBase64 (.getBytes string))))


(defn bytes->base64 [byte-aray]
  (org.apache.commons.codec.binary.Base64/encodeBase64String byte-aray))
(def byte-array->base64-string bytes->base64)

(defn string->input-stream [base64-string]
  (new
    org.apache.commons.codec.binary.Base64InputStream
    (io/string->input-stream base64-string)))

(defn input-stream->decoded-input-stream
  "Converts InputStream of Base64 encoded data into raw InputStream"
  [base64-input-stream]
  (new
    org.apache.commons.codec.binary.Base64InputStream
    base64-input-stream))

(defn input-stream->encoded-input-stream
  "Converts raw InputStream to Base64 encoded data InputStream"
  [input-stream]
  (new
    org.apache.commons.codec.binary.Base64InputStream
    input-stream
    true))

(defn output-stream->decoded-output-stream
  "Converts OutputStream of Base64 encoded data into raw OutputStream"
  [base64-output-stream]
  (new
    org.apache.commons.codec.binary.Base64OutputStream
    base64-output-stream))

(defn output-stream->encoded-output-stream
  "Converts OutputStream of raw data into Base64 encoded OutputStream"
  [output-stream]
  (new
    org.apache.commons.codec.binary.Base64OutputStream
    output-stream
    true))
