(defproject com.mungolab/clj-common "0.2.0-SNAPSHOT"
  :description "common functions"
  :url "https://github.com/vanjakom/clj-common"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :deploy-repositories [
                         ["clojars" {
                                      :url "https://clojars.org/repo"
                                      :sign-releases false}]]
  :dependencies [
                  [org.clojure/clojure "1.10.0"]

                  [org.clojure/core.async "0.4.490"]


                  [clj-time "0.11.0"]
                  [clj-http "2.2.0"]

                  [commons-io/commons-io "2.5"]
                  [commons-codec/commons-codec "1.10"]

                  ; metrics stack
                  [io.dropwizard.metrics/metrics-core "3.2.2"]
                  [io.dropwizard.metrics/metrics-graphite "3.2.2"]
                  [io.dropwizard.metrics/metrics-servlets "3.2.2"]
                  [org.eclipse.jetty/jetty-server "9.4.4.v20170414"]
                  [org.eclipse.jetty/jetty-servlet "9.4.4.v20170414"]

                  ; http stack
                  [compojure "1.5.0"]
                  [ring "1.4.0"]
                  [ring/ring-json "0.4.0" :exclusions [com.fasterxml.jackson.core/jackson-core]]

                  ; mail sending
                  [com.draines/postal "2.0.2"]
                  [javax.mail/mail "1.4.7"]

                  ; visualization
                  [incanter "1.5.7"]

                  [funcool/cats "2.1.0"]])
