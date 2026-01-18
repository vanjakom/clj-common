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

(defn br []
  "<br>")

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
   (configure-metric-width {} width))
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

(defn configure-hide-legend [metric]
  (assoc metric :hide-legend true))

(defn configure-show-legend [metric]
  (assoc metric :hide-legend false))

(defn render-metric [configuration metric]
  (let [timeframe (or (get configuration :timeframe) "2hours")
        graphite (get configuration :graphite)
        width (or (get configuration :metric-width) 600)
        height (or (get configuration :metric-height) 375)]
    (tag
     "div"
     {}
     (str
      (get metric :name)
      (br)
      (tag
       "img"
       {
        "src"
        (str graphite "/render?"
             "from=-" timeframe
             "&until=now"
             "&width=" width
             "&height=" height
             "&target=" (url-encode (get metric :metric))
             ;; "&title=" (url-encode (get metric :name))
             (when-let [hide-legend (get metric :hide-legend)]
               (str "&hideLegend=" hide-legend)))})
      "\n"
      (tag
       "div"
       {}
       (get metric :metric))
      (br)
      (br)))))

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
     {
      "style" "text-align:center;"}
     (map
      (partial render-metric configuration)
      metrics))]))

;;  metric construct functions
(defn fn-keep-last-value [rest]
  (str "keepLastValue(" rest ")"))

(defn fn-group-by-nodes [rest function & nodes]
  (str
   "groupByNodes(" rest ",\"" function "\"," (clojure.string/join "," nodes)")"))

(defn fn-highest-average [rest num-of-metrics]
  (str "highestAverage(" rest "," num-of-metrics ")"))

(defn fn-sum [rest]
  (str "sum(" rest ")"))

(defn fn-sum-series [rest]
  (str "sumSeries(" rest ")"))

(defn fn-integral [rest]
  (str "integral(" rest ")"))

(defn fn-summarize-day [rest]
  (str "summarize(" rest ",\"1day\", sum)"))

(defn fn-alias-by-node [rest node]
  (str "aliasByNode(" rest "," node ")"))

(defn fn-current-above-zero [rest]
  (str "currentAbove(" rest ", 0)"))

;; metric "type" extract
(defn metric-m5-rate [metric]
  (str metric ".m5_rate"))

(defn metric-mean [metric]
  (str metric ".mean"))

(defn metric-child [root & child]
  (clojure.string/join "." (concat root child)))
