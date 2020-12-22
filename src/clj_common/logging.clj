(ns clj-common.logging
  (:require
   clojure.pprint
   [clj-common.io :as io]
   [clj-common.localfs :as fs]
   [clj-common.path :as path]
   [clj-common.jvm :as jvm]
   [clj-common.edn :as edn]
   [clj-common.time :as time]))

; note cannot depend to clj-commmon.clojure

(def logger (agent nil))

#_(defn report
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
        (io/write-new-line output-stream)
        (.flush output-stream)
        output-stream)))
  nil)

(defn report
  "Uses agent to report to std"
  [object]
  (send
   logger
   (fn [_]
     (if (coll? object)
       (clojure.pprint/pprint object)
       (clojure.pprint/pprint {:message object}))
     nil))
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

#_(report-throwable (new Exception "test"))
