(ns clj-common.logging
  (:require [clj-common.localfs :as fs]
            [clj-common.path :as path]
            [clj-common.jvm :as jvm]
            [clj-common.edn :as edn]
            [clj-common.time :as time]))

(def logger (agent nil))

(defn report
  "Lazy opens <jvm-path>/logging for output"
  [object]
  (send
    logger
    (fn [possible-output-stream]
      (let [output-stream (if (some? possible-output-stream)
                            possible-output-stream
                            (let [fresh-output-stream
                                  (fs/output-stream-by-appending
                                    (path/child (jvm/jvm-path) "logging"))]
                              (edn/write-object
                                fresh-output-stream
                                {
                                  :status "logging started"
                                  :timestamp (time/timestamp-second)})
                              fresh-output-stream))]
        (if (coll? object)
          (edn/write-object output-stream object)
          (edn/write-object output-stream {:message object}))
        (.flush output-stream)
        output-stream)))
  nil)

(defn report-throwable
  ([t]
   (report-throwable {} t))
  ([info t]
   (report
     (assoc
       info
       :status :exception
       :class (.getName (.getClass t))
       :message (.getMessage t)
       :stack (map
                (fn [element]
                  (.toString element))
                (.getStackTrace t))))))
