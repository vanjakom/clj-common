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

;; 20260811
;; support for reading / writing of notemd header, tags declared with
;; [tag:#tag] apply to whole document, take a look at
;; https://github.com/vanjakom/notes-viewer/blob/master/notemd-definition.md
(defn parse-header-tag [line]
  (when (and (.startsWith line "[tag:") (.endsWith line "]"))
    (.substring line 5 (dec (count line)))))

(defn extract-header-tags [lines]
  (into #{} (filter some? (map parse-header-tag lines))))

(defn create-note [tags header content]
  {
   :id (uuid)
   :tags tags
   :header header
   :content content})

(defn tags [note] (:tags note))
(defn header [note] (:header note))
(defn content [note ] (:content note))

(defn update-tags [note tags]
  (assoc note :tags tags))

(defn update-header [note header]
  (assoc note :header header))

(defn read-notes [is default-tags]
  ;; claude
  ;; buffer accumulates lines of whatever comes before the next "# " line.
  ;; on hitting a "# " line ( note or section start ) three cases arise:
  ;; - buffer is empty ( first line of file is already "# " ) - nothing to
  ;;   flush, note header ( "# " itself ) is skipped
  ;; - buffer's first line starts with "# " - buffer holds a finished note,
  ;;   flush it via create-note, applying default-tags to both its tag set
  ;;   and, when title is long enough, into its rendered header text
  ;; - otherwise buffer holds the notemd preamble ( [notemd], [tag:#tag]
  ;;   lines and free text comments before the first note ) - not a note,
  ;;   so instead of flushing it, pull any [tag:#tag] declarations out of it
  ;;   and fold them into default-tags for the rest of the file
  ;; in all cases the just read "# " line becomes the start of the next
  ;; buffer, carried until the following "# " line repeats the decision
  (loop [lines (io/input-stream->line-seq is)
         notes []
         tags #{}
         default-tags default-tags
         buffer []]
    (if-let [line (first lines)]
      (if (.startsWith line "# ")
        (let [new-tags (parse-tags line)]
          (cond
            (empty? buffer)
            (recur (rest lines) notes new-tags default-tags [line])

            (.startsWith (first buffer) "# ")
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
             default-tags
             [line])

            ;; 20260811
            ;; buffer holds notemd header ( [notemd], [tag:#tag] lines and
            ;; free text comments ) before first note, not a note itself,
            ;; extract default tags declared there
            :else
            (recur
             (rest lines)
             notes
             new-tags
             (into default-tags (extract-header-tags buffer))
             [line])))
        (recur
         (rest lines)
         notes
         tags
         default-tags
         (conj buffer line)))
      (if (and (not (empty? buffer)) (.startsWith (first buffer) "# "))
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
        process-fn (get configuration :process-fn)
        ;; 20260811
        ;; set of lines to add at beginning of file, useful for exports
        ;; in case header or default-tags are used add "[notemd]" as first
        ;; line, take a look at
        ;; https://github.com/vanjakom/notes-viewer/blob/master/notemd-definition.md
        header (get configuration :header)
        ;; list of tags that are used on whole document
        default-tags (get configuration :default-tags)
        ;; explicit header / default-tags replace whatever preamble the
        ;; source file has, otherwise the original preamble ( content
        ;; before the first note ) has to be preserved verbatim, not
        ;; silently dropped
        has-explicit-header (or (not (empty? default-tags)) (not (empty? header)))]
    (context/trace context (str "processing: " path))
    (context/trace context (str "writing to: " output-path))
    (with-open [is (fs/input-stream path)
                os (fs/output-stream output-path)]
      (when has-explicit-header
        (io/write-line os "[notemd]")
        (doseq [tag (sort default-tags)]
          (io/write-line os (str "[tag:" tag "]")))
        (when (not (empty? header))
          (doseq [header-line (clojure.string/split header #"\n")]
            (io/write-line os header-line)))
        (io/write-line os ""))
      ;; copy with modifications of read-notes
      (let [write-note-fn (fn [note]
                            ;; writing header instead of serializing tags
                            ;; this requires tag manipulation of output in
                            ;; headers
                            (io/write-line os (:header note))
                            (io/write-line os (:content note))
                            (io/write-line os ""))
            write-preamble-fn (fn [buffer-lines]
                                 (when (and (not has-explicit-header) (seq buffer-lines))
                                   (doseq [buffer-line buffer-lines]
                                     (io/write-line os buffer-line))))]
        (loop [lines (io/input-stream->line-seq is)
               tags #{}
               buffer []]
          (if-let [line (first lines)]
            (if (.startsWith line "# ")
              (let [new-tags (parse-tags line)]
                (cond
                  (empty? buffer)
                  (recur (rest lines) new-tags [line])

                  (.startsWith (first buffer) "# ")
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

                  ;; buffer holds notemd preamble, not a note itself,
                  ;; preserve it verbatim unless an explicit header /
                  ;; default-tags was given to replace it with
                  :else
                  (do
                    (write-preamble-fn buffer)
                    (recur
                     (rest lines)
                     new-tags
                     [line]))))
              (recur
               (rest lines)
               tags
               (conj buffer line)))
            (when (and (not (empty? buffer)) (.startsWith (first buffer) "# "))
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

(defn prepare-note-github [note]
  (update-in
   note
   [:content]
   (fn [content]
     (let [line-seq (.split content "\n")]
       (clojure.string/join
        "\n"
        (map
         (fn [line]
           (if (.endsWith line "  ")
             line
             (str line "  ")))
         line-seq))))))

(defn- html-escape [text]
  (->
   text
   (clojure.string/replace "&" "&amp;")
   (clojure.string/replace "<" "&lt;")
   (clojure.string/replace ">" "&gt;")))

(defn- html-linkify [text]
  (clojure.string/replace
   text
   #"https?://\S+"
   (fn [url] (str "<a href=\"" url "\" target=\"_blank\">" url "</a>"))))

(defn header->title
  "Strips leading # marker and any #tag / @tag tokens from header line,
  keeping only display title"
  [header]
  (->>
   (clojure.string/split header #" ")
   (remove #(or (.startsWith % "#") (.startsWith % "@")))
   (clojure.string/join " ")))

(defn- header->title-html
  "header->title wrapped as <h1>"
  [header]
  (str "<h1>" (html-escape (header->title header)) "</h1>\n"))

(defn- content->html
  "Renders note content as html, plain lines are escaped and linkified
  with a <br> after each, lines fenced with ``` on their own become a
  single <pre> block, escaped but not linkified"
  [content]
  (loop [lines (clojure.string/split content #"\n" -1)
         in-code false
         html ""]
    (if-let [line (first lines)]
      (cond
        (= (clojure.string/trim line) "```")
        (recur
         (rest lines)
         (not in-code)
         (str html (if in-code "</pre>\n" "<pre class=\"code-block\">")))

        in-code
        (recur (rest lines) in-code (str html (html-escape line) "\n"))

        :else
        (recur
         (rest lines)
         in-code
         (str html (html-linkify (html-escape line)) "<br>\n")))
      html)))

(defn prepare-note-html
  "Renders note as a plain html string: header becomes an <h1> title ( #
  marker and any tags stripped ), content is html escaped, plain text
  lines get linkified and <br> separated, ```fenced``` lines become a
  single <pre class=\"code-block\"> block. configuration is a map of
  options, reserved for future use, currently none are read"
  [configuration note]
  (str
   (header->title-html (:header note))
   (content->html (:content note))))

(defn notes->html
  "Renders given notes as a single, self contained html page: monospace
  font, fenced ```code``` blocks shown in a bordered box, styled to look
  similar to a github rendered markdown page. configuration is a map of
  options, reserved for future use, currently none are read, passed
  through to prepare-note-html. Returns html as a string, caller is
  responsible for writing it out"
  [configuration notes]
  (str
   "<html>\n"
   "<head>\n"
   "<meta charset=\"UTF-8\" />\n"
   "<style>\n"
   "body {font-family: monospace; max-width: 900px; margin: 40px auto; padding: 0 20px;}\n"
   ".code-block {background: #f6f8fa; border: 1px solid #d0d7de; border-radius: 6px; padding: 16px; overflow-x: auto;}\n"
   "</style>\n"
   "</head>\n"
   "<body>\n"
   (apply
    str
    (map
     (fn [note]
       (str "<div class=\"note\">\n" (prepare-note-html configuration note) "\n</div>\n"))
     notes))
   "</body>\n"
   "</html>\n"))

(defn notes-path->html
  "Job wrapper around notes->html: reads notes file on path given in
  configuration and writes it, rendered as a single html page, to
  output-path given in configuration"
  [context]
  (let [configuration (context/configuration context)
        path (get configuration :path)
        output-path (get configuration :output-path)]
    (context/trace context (str "rendering: " path))
    (context/trace context (str "writing to: " output-path))
    (let [notes (with-open [is (fs/input-stream path)]
                  (read-notes is #{}))]
      (with-open [os (fs/output-stream output-path)]
        (io/write-string os (notes->html {} notes))))
    (context/trace context "rendering finished")))
