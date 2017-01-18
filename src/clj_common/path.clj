(ns clj-common.path)

; path represents seq of string or keyword elements

(defn path2string [path]
  (str "/" (clojure.string/join "/" path)))

(defn path4string [path-str]
  (let [splits (clojure.string/split path-str #"/")]
    (if (empty? (first splits))
      (into [] (rest splits))
      splits)))

(defn parent [path]
  (into [] (drop-last path)))

(defn child [path & components]
  (apply conj path components))

(defn name [path]
  (last path))

(defn extension [path]
  (let [path-name (name path)
        splits (clojure.string/split path-name #"\.")]
    (if (= (count splits) 2)
      (last splits)
      nil)))

(defn name-without-extension [path]
  (let [path-name (name path)
        splits (clojure.string/split path-name #"\.")]
    (if (= (count splits) 2)
      (first splits)
      path-name)))

(defn sort-by-name-without-extension
  "Sorts list of paths by part of name applying extract fn
  paths - list of paths to work on
  extract-fn - function for part of name extraction, gets name"
  [paths extract-fn]
  (sort-by
    #(extract-fn (name-without-extension %1))
    paths))
