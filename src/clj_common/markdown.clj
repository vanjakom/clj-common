(ns clj-common.markdown
  (:use
   clj-common.clojure))

;; simple markdown / wikitext parser
;; created for purpose of parsing wiki pages

(def sample
  [
   "Neki tekst"
   "[[Test test link]]"
   "{|"
   "|-"
   "| tag1"
   "| vrednost1"
   "|-"
   "| tag2"
   "| vrednost2"
   "|}"])

(defn parse-table-row
  [line-seq]
  (loop [line-seq line-seq
         content []]
    (if-let [line (first line-seq)]
      (do
        (println "row> " line)
        (cond
          (.startsWith line "| ")
          (recur 
           (rest line-seq)
           (conj content (.substring line 2)))

          (.startsWith line "|-")
          [line-seq content]

          (.startsWith line "|}")
          ;; give chance to table fn for closure
          [line-seq content]
          :else
          (throw (ex-info "Unexpected line" {:data line}))))
      [line-seq content])))

(defn parse-table
  "Accepts line-seq where first line is table start, parses table returning
  remaining of lines and table"
  [line-seq]
  (loop [line-seq line-seq
         content []]
    (if-let [line (first line-seq)]
      (do
        (println "table> " line)
        (cond
          (.startsWith line "{|")
          (recur (rest line-seq) content)
          
          (.startsWith line "|-")
          (let [[rest-line-seq row] (parse-table-row (rest line-seq))]
            (recur rest-line-seq (conj content row)))
        
          (.startsWith line "|}")
          [(rest line-seq) content]))
      [line-seq content])))

;; initially parse only tables

(defn parse-wikitext
  "Parses wikitext lines produced by clj-scraper.scrapers.org.wikipedia/title->wikitext"
  [line-seq]
  (loop [line-seq line-seq
         content []]
    (if-let [line (first line-seq)]
      (do
        (println "> " line)
        (if (.startsWith line "{|")
         ;; give chance parse-table to extract class from first line
          (do
            (println "parsing table")
            (let [[rest-line-seq table] (parse-table line-seq)]
              (recur
               rest-line-seq
               (conj content table))))
          (recur
           (rest line-seq)
           content)))
      content)))

#_(parse-wikitext sample)

#_(.startsWith (first sample) "a")

#_(def a (clj-scraper.scrapers.org.wikipedia/title->wikitext "sr" "Списак_записа_у_Србији"))

#_(take 4 a)
