(ns clj-common.jar
  (:require [clj-common.path :as path])
  (:require [clj-common.logging :as logging])
  (:require [clj-common.localfs :as fs])
  (:require [clj-common.io :as io])
  (:import java.util.jar.JarFile)
  (:import java.util.jar.JarEntry))

; thanks to
; https://stackoverflow.com/questions/28645436/idiomatic-clojure-to-copy-resources-from-running-jar-to-outside

(defn read-jar [path]
  (new JarFile (path/path->string path)))

(defn contents [path]
  (map
    (fn [file]
      (path/string->path (.getName file)))
    (enumeration-seq (.entries (read-jar path)))))

(defn file-as-stream [^JarFile jar relative-path]
  (.getInputStream
    jar
    (.getEntry jar (.substring (path/path->string relative-path) 1))))

(defn extract [jar-path to-path]
  (fs/mkdirs to-path)
  (let [jar (read-jar jar-path)]
    (doseq [file (enumeration-seq (.entries jar))]
      (let [file-path (path/string->path (.getName file))
            destination-path (apply
                               path/child
                               to-path
                               file-path)]
        (if (not (.isDirectory file))
          (do
            (fs/mkdirs (path/parent destination-path))
            (with-open [output-stream (fs/output-stream
                                        destination-path)]
              (io/copy-input-to-output-stream
                (.getInputStream jar file)
                output-stream))))))))


(comment
  (extract
  (path/string->path
        "/Users/vanja/.m2/repository/com/mungolab/clj-common/0.1.0-SNAPSHOT/clj-common-0.1.0-SNAPSHOT.jar")
  ["tmp" "jar-extract"])

  (logging/report "Test")
  (run!
    logging/report
    (contents
      (path/string->path
        "/Users/vanja/.m2/repository/com/mungolab/clj-common/0.1.0-SNAPSHOT/clj-common-0.1.0-SNAPSHOT.jar")))

  (require 'clj-common.io)
  (require 'clj-common.localfs)
  (with-open [input-stream (file-as-stream
                             (read-jar
                               (path/string->path
                                 "/Users/vanja/.m2/repository/com/mungolab/clj-common/0.1.0-SNAPSHOT/clj-common-0.1.0-SNAPSHOT.jar"))
                             ["clj_common" "io.clj"])
              output-stream (clj-common.localfs/output-stream ["tmp" "test"])]
    (clj-common.io/copy-input-to-output-stream
      input-stream
      output-stream))

  (read-jar
    (path/string->path
      "/Users/vanja/.m2/repository/com/mungolab/clj-common/0.1.0-SNAPSHOT/clj-common-0.1.0-SNAPSHOT.jar"))

)
