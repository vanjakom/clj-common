(ns clj-common.http-proxy
  (:use
   clj-common.clojure)
  (:require
   [clj-common.as :as as]
   [clj-common.http :as http]
   [clj-common.http-server :as server]))

(defn -main [& args]
  (let [destination-host (nth args 0)
        destination-port (as/as-long (nth args 1))
        port (as/as-long (nth args 2))]
    (println "running server on" port)
    (println "requests are sent to" destination-host "at" destination-port)
    (server/create-server
     port
     (fn [request]
       (if (= (:request-method request) :get)
         (let [url (str
                    "http://" destination-host
                    (when
                        destination-port
                      (str ":" destination-port))
                    (:uri request)
                    (when (:query-string request)
                      (str "?" (:query-string request))) )]
           (println "[GET]" url)
           (let [response (http/get-raw-as-stream url)]
             {
              :status (:status response)
              :body (:body response)}))
         {:status 404})))))

#_(-main "qa.ref-design.supplyframe.io" nil  "7071")
