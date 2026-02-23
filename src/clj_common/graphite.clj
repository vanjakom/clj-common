(ns clj-common.graphite
  (:use
   clj-common.clojure)
  (:require
   [clj-common.io :as io]
   [clj-common.localfs :as fs]))

(def default-timeframes
  ["30minutes" "1hours" "3hours" "6hours" "24hours" "3days" "7days" "30days"])

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
   :type :metric
   :metric metric
   :name name
   ;; 202260220 by default hide legend ( reported as text )
   :hide-legend true})

(defn section [name]
  {
   :type :section
   :name name})

(defn configure-hide-legend [metric]
  (assoc metric :hide-legend true))

(defn configure-show-legend [metric]
  (assoc metric :hide-legend false))

(defn metric-anchor [name]
  (let [lower (clojure.string/lower-case (str name))
        cleaned (clojure.string/replace lower #"[^a-z0-9]+" "-")
        trimmed (clojure.string/replace cleaned #"(^-|-$)" "")]
    (if (clojure.string/blank? trimmed)
      "metric"
      trimmed)))

(defn render-metric [configuration metric]
  (let [timeframe (or (get configuration :timeframe) "3hours")
        graphite (get configuration :graphite)
        width (or (get configuration :metric-width) 600)
        height (or (get configuration :metric-height) 375)
        anchor (metric-anchor (get metric :name))]
    (tag
     "div"
      {}
      (str
      (tag "a" {"id" anchor} (get metric :name))
      " "
      (tag "a" {"href" (str "#" anchor)} "share")
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

(defn render-section [configuration section]
  (tag
   "div"
   {"style" "font-size:1.2em;font-weight:bold;"}
   (tag "a" {"id" (metric-anchor (get section :name))} (get section :name))))

(defn render
  "Element could be metric or section."
  [configuration elements]
  (let [selected-timeframe (or (get configuration :timeframe) "3hours")]
    (tag
     "html"
     {}
     [(tag
       "head"
       {"title" (or (:name configuration) "Graphite dashboard")}
       ;; todo, complex, imgs src needs to be rewritten, we wnat simple
       ;; static html without need for js
       (script
        "
        var params = new URLSearchParams(window.location.search);
        var timeframe = params.get('timeframe') || '2hours';
        console.log('timeframe:', timeframe);
        function updateTimeframeLinks(selected) {
          var container = document.getElementById('timeframe-links');
          if (!container) { return; }
          var values = (container.getAttribute('data-timeframes') || '').split(',');
          container.innerHTML = '';
          values.forEach(function(value) {
            if (!value) { return; }
            if (value === selected) {
              var span = document.createElement('span');
              span.setAttribute('data-timeframe', value);
              span.textContent = value;
              container.appendChild(span);
            } else {
              var link = document.createElement('a');
              link.setAttribute('data-timeframe', value);
              link.setAttribute('href', '#');
              link.onclick = function() { return setTimeframe(value); };
              link.textContent = value;
              container.appendChild(link);
            }
            container.appendChild(document.createTextNode(' '));
          });
        }
        function setTimeframe(selected) {
          var images = document.getElementsByTagName('img');
          for (var i = 0; i < images.length; i++) {
            var img = images[i];
            if (img.src.indexOf('/render?') === -1) { continue; }
            try {
              var url = new URL(img.src);
              var params = url.searchParams;
              params.set('from', '-' + selected);
              url.search = params.toString();
              img.src = url.toString();
            } catch (e) {}
          }
          updateTimeframeLinks(selected);
          return false;
        }"))
      (tag
       "body"
       {"style" "text-align:center;"}
       (concat
         [(tag
           "div"
           {"style" "text-align:right;position:fixed;top:0;right:0;background:white;padding:4px;z-index:1000;"}
           (concat
            [(tag
              "div"
              {"id" "timeframe-links"
               "data-timeframes" (clojure.string/join "," default-timeframes)}
              (interpose
               " "
               (map
                (fn [timeframe]
                  (if (= timeframe selected-timeframe)
                    (tag "span" {"data-timeframe" timeframe} timeframe)
                    (tag
                     "a"
                     {"data-timeframe" timeframe
                      "href" "#"
                      "onclick" (str "return setTimeframe('" timeframe "');")}
                     timeframe)))
                default-timeframes)))]
            [(br) (br)]))
          (tag "div" {"style" "height:24px;"} "")
         (tag
          "div"
          {"style" "text-align:left;"}
          (map
           (fn [element]
             (str
              (when (= (:type element) :section)
                (br))
              (cond
                (= (:type element) :metric)
                (tag
                 "a"
                 {"href" (str "#" (metric-anchor (get element :name)))}
                 (get element :name))

                (= (:type element) :section)
                (get element :name)

                :else
                "")
              (br)))
           (filter
            (fn [element]
              (or
               (= (:type element) :metric)
               (= (:type element) :section)))
            elements)))
         (br)
         (br)]
        (map
         (fn [element]
           (cond
             (= (:type element) :metric)
             (render-metric configuration element)

             (= (:type element) :section)
             (render-section configuration element)

             :else
             (br)))
         elements)))])))

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

(defn fn-exclude [rest pattern]
  (str "exclude(" rest ",\"" pattern "\")"))

(defn fn-summarize-meter-count-day [count-metric]
  (str
   "summarize(nonNegativeDerivative(keepLastValue("
   count-metric
   ")), \"1d\",\"sum\",false)"))

;; metric "type" extract
(defn metric-m5-rate [metric]
  (str metric ".m5_rate"))

(defn metric-mean [metric]
  (str metric ".mean"))

(defn metric-child [root & child]
  (clojure.string/join "." (concat root child)))
