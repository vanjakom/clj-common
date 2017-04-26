(ns clj-common.metrics)


(defn create-console-metrics []
  (let [counters (atom {})]
    {
      :inc-counter
      (fn [name]
        (let [new-val (swap! counters #(assoc %1 name (inc (get %1 name 0))))]
          (println "counter: " name ": " (get new-val name))))
      :report-timer
      (fn [name duration]
        (println "timer: " name ": " duration " ms"))
      :counters
      (fn []
        @counters)}))

(defn create-codahale-metrics []
  (let [metrics-registry (new com.codahale.metrics.MetricRegistry)]
    {
      :inc-counter
      (fn [name]
        (.inc (.counter metrics-registry name)))
      :report-timer
      (fn [name duration]
        (.update (.timer metrics-registry name) duration java.util.concurrent.TimeUnit/MILLISECONDS))

      ; codahale specific
      :metrics-registry metrics-registry}))

(defn create-and-attach-console-reporter [metrics]
  (let [console-reporter (->
                           (com.codahale.metrics.ConsoleReporter/forRegistry (:metrics-registry metrics))
                           (.convertDurationsTo java.util.concurrent.TimeUnit/MILLISECONDS)
                           (.convertRatesTo java.util.concurrent.TimeUnit/SECONDS)
                           (.build))]
    (.start console-reporter 10 java.util.concurrent.TimeUnit/SECONDS)
    console-reporter))

(defn create-and-attach-servlet-reporter [metrics servlet-port]
  (let [server (new org.eclipse.jetty.server.Server servlet-port)
        servlet-context-handler (new org.eclipse.jetty.servlet.ServletContextHandler server "/metrics" false false)]
    (.setAttribute
      (.getServletContext servlet-context-handler)
      com.codahale.metrics.servlets.MetricsServlet/METRICS_REGISTRY
      (:metrics-registry metrics))
    (.addServlet
      servlet-context-handler
      com.codahale.metrics.servlets.MetricsServlet
      "/registry")
    (.addServlet
      servlet-context-handler
      com.codahale.metrics.servlets.ThreadDumpServlet
      "/threads")
    (.start server)))

(defn create-and-attach-graphite-reporter [metrics graphite-host graphite-port prefix]
  (let [graphite (new com.codahale.metrics.graphite.Graphite graphite-host graphite-port)
        reporter (->
                   (com.codahale.metrics.graphite.GraphiteReporter/forRegistry (:metrics-registry metrics))
                   (.prefixedWith prefix)
                   (.convertRatesTo java.util.concurrent.TimeUnit/SECONDS)
                   (.convertDurationsTo java.util.concurrent.TimeUnit/MILLISECONDS)
                   (.filter com.codahale.metrics.MetricFilter/ALL)
                   (.build graphite))]
    (.start reporter 1 java.util.concurrent.TimeUnit/MINUTES)))

(def ^:dynamic *metrics* (create-console-metrics))

(defn inc-counter [name]
  ((:inc-counter *metrics*) name))

(defn report-timer [name duration]
  ((:report-timer *metrics*) name duration))

(defmacro eval-and-report-timer [name & exprs]
  `(let [start# (System/nanoTime)]
    (let [result# ~@exprs
          duration# (/ (- (System/nanoTime) start#) 1000000.0)]
      (report-timer ~name duration#)
      result#)))

(defn get-counters []
  ((:counters *metrics*)))


