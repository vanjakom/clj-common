(ns clj-common.path)

; path represents seq of string or keyword elements
; path's are always absolute, no need to start with /

(defn path->string [path]
  (str "/" (clojure.string/join "/" path)))

(defn string->path [path-str]
  (let [splits (clojure.string/split path-str #"/")]
    (if (empty? (first splits))
      (into [] (rest splits))
      splits)))

; old naming
(def path2string path->string)

(def path4string string->path)

(defn parent [path]
  (into [] (drop-last path)))

(defn child [path & components]
  (apply conj path components))

(defn name [path]
  (last path))

(defn name->name-extension [name]
  (let [last-index (.lastIndexOf name ".")]
    (if (>= last-index 0)
      [(.substring name 0 last-index) (.substring name (inc last-index))]
      [name nil])))

(defn extension [path]
  (let [[name-without-extension extension] (name->name-extension (name path))]
    extension))

(defn name-without-extension [path]
  (let [[name-without-extension extension] (name->name-extension (name path))]
    name-without-extension))

(defn sort-by-name-without-extension
  "Sorts list of paths by part of name applying extract fn
  paths - list of paths to work on
  extract-fn - function for part of name extraction, gets name"
  [paths extract-fn]
  (sort-by
    #(extract-fn (name-without-extension %1))
    paths))
