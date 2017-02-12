(ns clj-common.hash)

(def md5-digest (java.security.MessageDigest/getInstance "MD5"))

(defn md5-bytes [bytes]
  (.toString
    (new java.math.BigInteger
         1
         (.digest md5-digest bytes))
    16))

(defn md5-string [string]
  (md5-bytes (.getBytes string)))
