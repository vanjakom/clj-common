(ns clj-common.base64)

(def decoder (java.util.Base64/getDecoder))

(defn base64->byte-array [^String base64-as-string]
  (org.apache.commons.codec.binary.Base64/decodeBase64 (.getBytes base64-as-string)))

(defn base64->string [^String base64-as-string]
  (new String (base64->byte-array base64-as-string)))
