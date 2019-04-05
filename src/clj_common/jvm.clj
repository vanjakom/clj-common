(ns clj-common.jvm
  (:require
    [clojure.java.io :as io]
    [clj-common.path :as path]
    [clj-common.localfs :as fs]))

(defn get-memory []
  (let [runtime (Runtime/getRuntime)
        free-memory (.freeMemory runtime)
        max-memory (.maxMemory runtime)
        total-memory (.totalMemory runtime)]
    {
      :free-memory free-memory
      :max-memory max-memory
      :total-memory total-memory}))

(defn threads []
  (let [stack-traces (Thread/getAllStackTraces)]
    (into [] (.keySet stack-traces))))
(def get-threads threads)

(defn thread []
  (Thread/currentThread))

(defn thread-name []
  (.getName (Thread/currentThread)))

(defn interrupt-thread [thread-name]
  (.interrupt
   (first
    (filter #(= (.getName %) thread-name) (clj-common.jvm/threads)))))

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

(defn pid []
  (.getName (java.lang.management.ManagementFactory/getRuntimeMXBean)))
(def get-pid pid)

(defn get-hostname []
  (.getHostName (java.net.InetAddress/getLocalHost)))

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


(defn classpath []
  (System/getProperty "java.class.path"))

(def get-classpath classpath)

(defn classpath-as-path-seq []
  (map
    path/string->path
    (.split
      (classpath)
      ":")))

(defn home-path []
  (path/string->path
    (System/getProperty "user.home")))


; todo potential problem with loader used, using context class loader
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

(defn fork-process
  "Forks process and collects std out and std err"
  [statement]
  (let [process (.exec
                 (Runtime/getRuntime)
                 statement)]
    [(.getInputStream process) (.getErrorStream process)]))

(defn random-uuid [] (.toString (java.util.UUID/randomUUID)))

(defn classloader-from-path [parent & path-seq]
  ;; check paths to ensure there is no mistake
  (doseq [path path-seq]
    (when-not (fs/exists? path)
      (throw (ex-info "Path doesn't exist" {:path path}))))
  (new
   java.net.URLClassLoader
   (into-array
    java.net.URL
    (map
     #(new java.net.URL (str "file://"(path/path->string %)))
     path-seq))
   parent))

(defn map->java-hash-map [map]
  (let [java-hash-map (new java.util.HashMap)]
    (doseq [[key value] map]
      (.put java-hash-map key value))
    java-hash-map))
