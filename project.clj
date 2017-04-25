(defproject com.mungolab/clj-common "0.1.0-SNAPSHOT"
  :description "common functions"
  :url "https://github.com/vanjakom/clj-common"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                  [org.clojure/clojure "1.8.0"]
                  [lein-light-nrepl "0.3.2"]

                  [clj-time "0.11.0"]
                  [clj-http "2.2.0"]

                  [commons-io/commons-io "2.5"]

                  [io.dropwizard.metrics/metrics-core "3.1.2"]
                  [io.dropwizard.metrics/metrics-graphite "3.1.2"]
                  [io.dropwizard.metrics/metrics-servlets "3.1.2"]
                  [org.eclipse.jetty/jetty-server "9.3.4.v20151007"]
                  [org.eclipse.jetty/jetty-servlet "9.3.4.v20151007"]]
  :repl-options {
                  :nrepl-middleware [lighttable.nrepl.handler/lighttable-ops]})
