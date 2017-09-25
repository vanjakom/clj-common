(ns clj-common.jvm)

(require '[clojure.java.io :as io])

(require '[clj-common.path :as path])

(defn get-memory []
  (let [runtime (Runtime/getRuntime)
        free-memory (.freeMemory runtime)
        max-memory (.maxMemory runtime)
        total-memory (.totalMemory runtime)]
    {
      :free-memory free-memory
      :max-memory max-memory
      :total-memory total-memory}))

(defn get-threads []
  (let [stack-traces (Thread/getAllStackTraces)]
    (into [] (.keySet stack-traces))))

(defn print-memory []
  (let [{
          free-memory :free-memory
          max-memory :max-memory
          total-memory :total-memory}
        (get-memory)]
    (println
      "Memory: "
      (int (/ free-memory 1024 1024)) "MB free, "
      (int (/ max-memory 1024 1024)) "MB max, "
      (int (/ total-memory 1024 1024)) "MB total")))

(defn get-pid []
  (.getName (java.lang.management.ManagementFactory/getRuntimeMXBean)))

(defn jvm-path []
  (into
    []
    (drop-last
      (path/string->path
        (.getAbsolutePath
          (new java.io.File "."))))))

; old implementation
;(defn get-jvm-path
;  "Returns path from which JVM is started"
;  []
;  (path/path4string (System/getProperty "user.dir")))


(defn get-classpath []
  (System/getProperty "java.class.path"))

(defn resource-as-stream [path-in-jar]
  (if-let [resource-url (io/resource (.substring (path/path->string path-in-jar) 1))]
    (io/input-stream resource-url)))

(defn print-threads []
  (let [threads (get-threads)]
    (println "Threads: ")
    (doseq [thread threads]
      (println "Thread: " (.getName thread) " state: " (.getState thread)))))

(defn environment-variables []
  (System/getenv))

(defn environment-variable [name]
  (System/getenv name))


