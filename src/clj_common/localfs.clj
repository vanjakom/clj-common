(ns clj-common.localfs
  (:require [clj-common.path :as path]))

(defn exists?
  "Checks if path exists on local fs"
  [path]
  (.exists (new java.io.File (path/path->string path))))

(defn mkdirs
  "Ensures given path exists, making all non existing dirs"
  [path]
  (.mkdirs (new java.io.File (path/path->string path))))

(defn move
  "Performs move of file"
  [src-path dest-path]
  ;; old implementation, not working across drives
  #_(when-not (.renameTo
               (new java.io.File (path/path->string src-path))
               (new java.io.File (path/path->string dest-path)))
      (throw (ex-info
              "Unable to move file"
              {
               :source src-path
               :destination dest-path})))

  (let [source (java.nio.file.Paths/get
                (path/path->string src-path) (into-array String []))
        destination (java.nio.file.Paths/get
                     (path/path->string dest-path) (into-array String []))]
    (java.nio.file.Files/move
     source
     destination
     (into-array [java.nio.file.StandardCopyOption/REPLACE_EXISTING]))))

#_(move ["tmp" "test"] ["tmp" "move"])


(defn copy
  "Performs copy of file"
  [src-path dest-path]
  (let [source (java.nio.file.Paths/get
                (path/path->string src-path) (into-array String []))
        destination (java.nio.file.Paths/get
                     (path/path->string dest-path) (into-array String []))]
    (java.nio.file.Files/copy
     source
     destination
     (into-array [java.nio.file.StandardCopyOption/REPLACE_EXISTING]))))

#_(move ["tmp" "test"] ["tmp" "copy"])

(defn delete
  "Removes file or directory"
  [path]
  (let [file (new java.io.File (path/path->string path))]
    (when (.exists file)
      (org.apache.commons.io.FileUtils/forceDelete file))))

(defn input-stream
  "Creates input stream for given path"
  ^java.io.InputStream
  [path]
  (new java.io.FileInputStream (path/path->string path)))

(defn output-stream
  "Creates output stream for given path"
  ^java.io.OutputStream
  [path]
  (mkdirs (path/parent path))
  (new java.io.FileOutputStream ^String (path/path->string path)))

(defn output-stream-by-appending
  "Creates output stream for given path by appending"
  [path]
  (new java.io.FileOutputStream (path/path->string path) true))

(defn is-directory
  "Checks if given path represents directory"
  [path]
  (let [path-string (path/path->string path)
        file-object (new java.io.File path-string)]
    (.isDirectory file-object)))

(defn relative-path->path [relative-path]
  (.getCanonicalPath (new java.io.File relative-path)))

(defn list
  "List paths on given path if directory, if file or doesn't exist empty list is returned"
  [path]
  (if
    (is-directory path)
    (map
      (partial path/child path)
      (.list (new java.io.File (path/path->string path))))
    '()))


(.getPath
 (java.nio.file.FileSystems/getDefault)
 "tmp"
 (into-array String ["tmp" "input"]))

(defn link
  "Creates symoblic link"
  [target-path link-path]
  (let [file-system (java.nio.file.FileSystems/getDefault)
        path->nio-path #(.getPath file-system (str "/" (first %)) (into-array String (rest %)))]
    (java.nio.file.Files/createSymbolicLink
     (path->nio-path link-path)
     (path->nio-path target-path)
     (into-array java.nio.file.attribute.FileAttribute []))))

#_(link ["tmp" "input"] ["tmp" "link"])
