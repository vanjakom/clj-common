(ns clj-common.notemd
  (:use clj-common.clojure)
  (:require
   [clj-common.as :as as]
   [clj-common.context :as context]
   [clj-common.io :as io]
   [clj-common.localfs :as fs]))

(defn parse-tags [line]
  (let [line (if (.startsWith line "# ")
               (.substring line 2)
               line)
        tags (loop [line line
                    tags #{}
                    current ""]
               (if-let [c (first line)]
                 (if (or (= c \#) (= c \@))
                   (if (not (empty? current))
                     (recur
                      (rest line)
                      (conj tags (.trim current))
                      (str c))
                     (recur
                      (rest line)
                      tags
                      (str c)))
                   ;; in case of space report tag
                   (if (= c \ )
                     (recur
                      (rest line)
                      (if (not (empty? current))
                        (conj tags (.trim current))
                        tags)
                      "")
                    (recur
                     (rest line)
                     tags
                     (str current c))))
                 (if (not (empty? current))
                   (conj tags (.trim current))
                   tags)))]
    (into #{} (filter #(or (.startsWith % "#") (.startsWith % "@")) tags) )))

#_(parse-tags "# #list #divcibare #to") ;; #{"#divcibare" "#to" "#list"}
#_(parse-tags "# notes concept") ;; #{}
;; fix on 20240919
#_(parse-tags "# #20240917 #disha geospatial") ;; #{"#20240917" "#disha"}
#_(parse-tags "# #a #b c d #e") ;; #{"#a" "#b" "#e"}

(defn create-note [tags header content]
  {
   :id (uuid)
   :tags tags
   :header header
   :content content})

(defn read-notes [is default-tags]
  (loop [lines (io/input-stream->line-seq is)
         notes []
         tags #{}
         buffer []]
    (if-let [line (first lines)]
      (if (.startsWith line "# ")
        (let [new-tags (parse-tags line)]
          (if (not (empty? buffer))
            (recur
             (rest lines)
             (conj
              notes
              (create-note
               (into tags default-tags)
               ;; 20260301
               ;; adding default tags from file to header
               (if (> (count (first buffer))2)
                 (str
                  "# "
                  (clojure.string/join " " (sort default-tags))
                  " "
                  (.substring (first buffer) 2))
                 ;; only few of problematic / invalid notes
                 (first buffer))
               (clojure.string/join "\n" (rest buffer))))
             new-tags
             [line])
            (recur
             (rest lines)
             notes
             new-tags
             [])))
        (recur
         (rest lines)
         notes
         tags
         (conj buffer line)))
      (if (not (empty? buffer))
        (conj
         notes
         (create-note
          (into tags default-tags)
          ;; 20260301
          ;; adding default tags from file to header
          (if (> (count (first buffer))2)
            (str
             "# "
             (clojure.string/join " " (sort default-tags))
             " "
             (.substring (first buffer) 2))
            ;; only few of problematic / invalid notes
            (first buffer))
          (clojure.string/join "\n" (rest buffer))))
        notes))))

(defn process-notes-path
  "Reads notes file respecting structure and order and gives possibility
  to process-fn to alter note. Output is stored in output path.
  Note: currently state of process fn is not supported, must be stored
  externally. This could be useful if order of notes is required."
  ;; todo maybe to support output over process-fn
  ;; todo multiple args process fn ( start, process, close )
  ;; todo support same path / output-path ( in place process )
  [context]
  (let [configuration (context/configuration context)
        path (get configuration :path)
        output-path (get configuration :output-path)
        process-fn (get configuration :process-fn)]
    (context/trace context (str "processing: " path))
    (context/trace context (str "writing to: " output-path))
    (with-open [is (fs/input-stream path)
                os (fs/output-stream output-path)]
      ;; copy with modifications of read-notes
      (let [write-note-fn (fn [note]
                            ;; writing header instead of serializing tags
                            (io/write-line
                             os
                             (:header note))
                            (io/write-line
                             os
                             (:content note)))]
        (loop [lines (io/input-stream->line-seq is)
               tags #{}
               buffer []]
          (if-let [line (first lines)]
            (if (.startsWith line "# ")
              (let [new-tags (parse-tags line)]
                (if (not (empty? buffer))
                  (let [note (process-fn
                              (create-note
                               tags
                               (first buffer)
                               (clojure.string/join "\n" (rest buffer))))]
                    (when note (write-note-fn note))
                    (recur
                     (rest lines)
                     new-tags
                     [line]))
                  (recur
                   (rest lines)
                   new-tags
                   [])))
              (recur
               (rest lines)
               tags
               (conj buffer line)))
            (if (not (empty? buffer))
              (let [note (process-fn
                          (create-note
                           tags
                           (first buffer)
                           (clojure.string/join "\n" (rest buffer))))]
                (when note (write-note-fn note))))))))
    (context/trace context "processing finished")))

;; test extract for zanimljiva-geografija:kako-mapirati
#_(process-notes-path
   (context/create-stdout-context
    {
     :path ["Users" "vanja" "projects" "notes" "notes.md"]
     :output-path ["Users" "vanja" "projects" "zanimljiva-geografija" "blog" "tags.md"]
     :process-fn (fn [note]
                   (when (and
                          (contains? (:tags note) "#osm")
                          (contains? (:tags note) "#map")
                          (contains? (:tags note) "#tag")
                          (contains? (:tags note) "#export"))
                     note))}))

(defn parse-date [tag]
  ;; #20240909
  (when
      (and
       (= (count tag) 9)
       ;; todo
       ;; check month and day range
       (.startsWith tag "#2"))
      (as/as-long (.substring tag 1))))

(defn date [note]
  (first (filter some? (map parse-date (:tags note)))))

(defn lines [note]
  (.split
   (:content note)
   "\n"))

(defn ignore-comment-lines [note]
  (filter
   #(not (.startsWith % ";"))
   (lines note)))

(defn title [note]
  ;; todo
  )

(defn search [dataset search-tags]
  (filter
   (fn [note]
     (=
      (count search-tags)
      (count
       (filter
        #(contains? (:tags note) %)
        search-tags))))
   dataset))

