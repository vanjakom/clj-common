(ns clj-common.graphite
  (:use
   clj-common.clojure)
  (:require
   [clj-common.io :as io]
   [clj-common.localfs :as fs]))

(defn indent [value]
  (str "\t" value))

(defn tag 
  ([name attribute-map value]
   (let [attribute-str (reduce
                        (fn [state [key value]]
                          (str state " " key "=\"" value "\""))
                        ""
                        attribute-map)]
     (if (or (nil? value) (string? value))
       (str "<" name attribute-str ">" value "</" name ">\n")
       (str
        "<" name attribute-str ">\n"
        (clojure.string/join value)
        "</" name ">\n"))))
  ([name attribute-map]
   (tag name attribute-map nil))
  ([name]
   (tag name nil nil)))

(defn script
  [& statement-seq]
  (str
   "<script>"
   (clojure.string/join
    "\n"
    statement-seq)
   "</script>"))

(defn configure-graphite
  ([host port]
   (configure-graphite {} host port))
  ([configuration host port]
   (assoc configuration :graphite (str "http://" host ":" port "/") )))

(defn configure-name
  ([name]
   (configure-name {} name))
  ([configuration name]
   (assoc configuration :name name)))

(defn configure-metric-width
  ([width]
   (configure-graph-width {} width))
  ([configuration width]
   (assoc configuration :metric-width width)))

(defn configure-metric-height
  ([height]
   (configure-metric-height {} height))
  ([configuration height]
   (assoc configuration :metric-height height)))

(defn configure-timeframe
  ([timeframe]
   (configure-timeframe {} timeframe))
  ([configuration timeframe]
   (assoc configuration :timeframe timeframe)))

(defn metric [metric name]
  {
   :metric metric
   :name name})

(defn render-metric [configuration metric]
  (let [timeframe (or (get configuration :timeframe) "2hours")
        graphite (get configuration :graphite)
        width (or (get configuration :metric-width) 400)
        height (or (get configuration :metric-height) 250)]
    (tag
     "div"
     {}
     (str
      (tag
       "img"
       {
        "src"
        (str graphite "/render?"
             "from=-" timeframe
             "&until=now"
             "&width=" width
             "&height=" height
             "&target=" (get metric :metric)
             "&title=" (url-encode (get metric :name)))})
      "\n"
      (tag
       "div"
       {}
       (get metric :metric))))))

(defn render [configuration metrics]
  (tag
   "html"
   {}
   [(tag
     "head"
     {
      "title" (or (:name configuration) "Graphite dashboard")}
     ;; todo, complex, imgs src needs to be rewritten, we wnat simple
     ;; static html without need for js
     (script
      "
      var params = new URLSearchParams(window.location.search);
      var timeframe = params.get('timeframe') || '2hours';
      console.log('timeframe:', timeframe);"))
    (tag
     "body"
     {}
     (map
      (partial render-metric configuration)
      metrics))]))

;;  metric construct functions
(defn keep-last-value [rest]
  (str "keepLastValue(" rest ")"))

;; metric "type" extract
(defn m5-rate [metric]
  (str metric ".m5_rate"))

