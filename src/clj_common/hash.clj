(ns clj-common.hash)

(def md5-digest (java.security.MessageDigest/getInstance "MD5"))

(defn md5-bytes [bytes]
  (.toString
    (new java.math.BigInteger
         1
         (.digest md5-digest bytes))
    16))

(defn md5-input-stream ^bytes
  [^java.io.InputStream is]
  (let [md (java.security.MessageDigest/getInstance "MD5")
        buffer (byte-array 8192)]
    (loop []
      (let [n (.read is buffer)]
        (when (pos? n)
          (.update md buffer 0 n)
          (recur))))
    (.digest md)))

(defn md5-string [string]
  (md5-bytes (.getBytes string)))

(def string->md5-string md5-string)
(def string->md5 md5-string)
