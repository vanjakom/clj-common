(ns clj-common.markdown
  (:use
   clj-common.clojure))

;; simple markdown / wikitext parser
;; created for purpose of parsing wiki pages

(defn parse-wikitext
  "Parses wikitext lines produced by clj-scraper.scrapers.org.wikipedia/title->wikitext"
  [line-seq]
  (reduce
   (fn [[content in-table in-row] line]

     
     
     )
   [[] false false]
   line-seq))


#_(def a (clj-scraper.scrapers.org.wikipedia/title->wikitext "sr" "Списак_записа_у_Србији"))

#_(take 4 a)
