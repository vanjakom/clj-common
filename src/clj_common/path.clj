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

(defn confirm-schema
  "Tests if path confirms to schema, and if it's returns path representation.
  Starts from end giving possibility for arbitrary prefix ( will be stored
  in :prefix-path )"
  [schema path]
  (let [[rest-path match]
        (reduce
          (fn [[rest-path match] schema-fn]
            (let [candidate (first rest-path)]
             (if-let [new-match (schema-fn match candidate)]
               [(rest rest-path) new-match]
               (reduced [rest-path nil]))))
          [(reverse path) {}]
          (reverse schema))]
    (if (some? match)
      (assoc match :prefix-path (reverse rest-path))
      nil)))

(defn schema-single-string [keyword]
  (fn [match part]
    (if (string? part)
      (assoc match keyword part)
      nil)))

(defn schema-name-with-extension [required-extension]
  (fn [match part]
    (let [[name extension] (name->name-extension part)]
      (if (and (some? name) (= extension required-extension))
        (assoc
          match
          :name name
          :extension extension)
        nil))))

(defn schema-or [& schema-fn-seq]
  (fn [match part]
    (first
      (filter
        some?
        (map
          #(% match part)
          schema-fn-seq)))))

(comment
  ((schema-name-with-extension "NEF") {} "DSC_100.NEF")

  (confirm-schema
    [(schema-single-string :hard-tag)
     (schema-or (schema-name-with-extension "NEF") (schema-name-with-extension "JPG"))]
    ["Users" "vanja" "Pictures" "2018.01 - Log" "DSC_4361.NEF"])
  )





