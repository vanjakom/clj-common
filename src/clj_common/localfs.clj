(ns clj-common.localfs)

(use 'clj-common.path)

(defn exists
  "Checks if path exists on local fs"
  [path]
  (.exists (new java.io.File (path2string path))))

(defn mkdirs
  "Ensures given path exists, making all non existing dirs"
  [path]
  (.mkdirs (new java.io.File (path2string path))))

(defn input-stream
  "Creates input stream for given path"
  [path]
  (new java.io.FileInputStream (path2string path)))

(defn output-stream
  "Creates output stream for given path"
  [path]
  (new java.io.FileOutputStream (path2string path)))

(defn is-directory
  "Checks if given path represents directory"
  [path]
  (let [path-string (path2string path)
        file-object (new java.io.File path-string)]
    (.isDirectory file-object)))

(defn list
  "List paths on given path if directory, if file or doesn't exist empty list is returned"
  [path]
  (if
    (is-directory path)
    (map
      (partial child path)
      (.list (new java.io.File (path2string path))))
    '()))

