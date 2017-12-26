(ns clj-common.logging)

(require '[clj-common.localfs :as fs])
(require '[clj-common.path :as path])
(require '[clj-common.jvm :as jvm])
(require '[clj-common.edn :as edn])

(def logger (agent nil))

(defn report
  "Lazy opens <jvm-path>/logging for output"
  [object]
  (send
    logger
    (fn [possible-output-stream]
      (let [output-stream (if (some? possible-output-stream)
                            possible-output-stream
                            (fs/output-stream
                              (path/child
                                (jvm/jvm-path)
                                "logging")))]
        (if (coll? object)
          (edn/write-object output-stream object)
          (edn/write-object output-stream {:message object}))
        (.flush output-stream)
        output-stream)))
  nil)

(defn report-throwable [info t]
  (report
    (assoc
      info
      :status :exception
      :class (.getName (.getClass t))
      :message (.getMessage t)
      :stack (map
               (fn [element]
                 (.toString element))
               (.getStackTrace t)))))
