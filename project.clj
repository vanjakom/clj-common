(defproject com.mungolab/clj-common "0.3.1"
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
                 [clj-http "3.12.3"]

                 [commons-lang/commons-lang "2.6"]                 
                 [commons-io/commons-io "2.5"]
                 [commons-codec/commons-codec "1.10"]

                 ;; metrics stack
                 [io.dropwizard.metrics/metrics-core "3.2.2"]
                 [io.dropwizard.metrics/metrics-graphite "3.2.2"]
                 [io.dropwizard.metrics/metrics-servlets "3.2.2"]
                 ;; using jetty from ring/ring-jetty-adapter
                 ;;[org.eclipse.jetty/jetty-server "9.4.4.v20170414"]
                 ;;[org.eclipse.jetty/jetty-servlet "9.4.4.v20170414"]

                 ;; http stack
                 [compojure "1.5.0"]
                 [ring/ring-core "1.14.1"]
                 [ring/ring-jetty-adapter "1.14.1"]
                 [ring/ring-json "0.5.1"]
                 [ring/ring-codec "1.3.0"]

                 ;; json
                 [com.fasterxml.jackson.core/jackson-core "2.14.2"]
                 [com.fasterxml.jackson.core/jackson-databind "2.14.2"]
                 [com.fasterxml.jackson.core/jackson-annotations "2.14.2"]

                 ;; mail sending
                 [com.draines/postal "2.0.2"]
                 [javax.mail/mail "1.4.7"]

                 ;; visualization
                 [incanter "1.5.7"]

                 ;; is it used?
                 [funcool/cats "2.1.0"]

                 [com.mungolab/clj-common-java "0.3.0"]])
