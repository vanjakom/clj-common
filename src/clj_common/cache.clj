(ns clj-common.cache
  (:require [clj-common.localfs :as fs])
  (:require [clj-common.io :as io])
  (:require [clj-common.path :as path]))

; different implementations of cache, two functions
; f(key) -> value or nil and f(key, value) -> void


; usage
; clj-scraper.scrapers.retrieve

(defn create-in-memory-cache []
  (let [cache (atom {})]
    (fn
      ([key]
       (get @cache key))
      ([key value]
       (swap!
         cache
         (fn [cache]
           (assoc cache key value)))))))

(defn create-local-fs-cache
  "Uses given path for storing of value under key-fn(key) file. value is serialized
  and deserialized when stored on disk
  value-serialize-fn - f(object) -> bytes
  value-deserialize-fn - f(bytes) -> object"
  [{
     cache-path :cache-path
     key-fn :key-fn
     value-serialize-fn :value-serialize-fn
     value-deserialize-fn :value-deserialize-fn
     :or {
           key-fn identity
           value-decode-fn identity
           value-encode-fn identity}}]
  (fn
    ([key]
     (let [path (path/child
                  cache-path
                  (key-fn key))]
       (if (fs/exists? path)
         (with-open [input-stream (fs/input-stream path)]
           (value-deserialize-fn (io/input-stream->bytes input-stream))))))
    ([key value]
     (let [path (path/child
                  cache-path
                  (key-fn key))]
       (with-open [output-stream (fs/output-stream path)]
         (io/write output-stream (value-serialize-fn value)))))))


(comment
  (let [cache (create-local-fs-cache
                {
                  :cache-path (path/string->path "/tmp/cache")
                  :key-fn clj-common.hash/string->md5-string
                  :value-serialize-fn clj-common.json/serialize
                  :value-deserialize-fn clj-common.json/deserialize})]

    (cache "test1")

    (cache "test1" {:a 10})


    (cache "test1"))

  (clj-common.json/deserialize
    (clj-common.json/serialize {:a 10 :b (.getBytes "test")})))
