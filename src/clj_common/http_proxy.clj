(ns clj-common.http-proxy
  (:use
   clj-common.clojure)
  (:require
   [clj-http.client :as clj-http]
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
       (try
         (cond
          (= (:request-method request) :get)
          (let [url (str
                     (if (.contains destination-host "://")
                       destination-host
                       (str "http://" destination-host))
                     (when
                         (> destination-port 0)
                         (str ":" destination-port))
                     (:uri request)
                     (when (:query-string request)
                       (str "?" (:query-string request))) )]
            (println "[GET]" url)
            (let [response (http/get-raw-as-stream url)]
              {
               :status (:status response)
               :body (:body response)}))

          (= (:request-method request) :post)
          (let [url (str
                     (if (.contains destination-host "://")
                       destination-host
                       (str "http://" destination-host))
                     (when
                         (> destination-port 0)
                         (str ":" destination-port))
                     (:uri request)
                     (when (:query-string request)
                       (str "?" (:query-string request))) )]
            (println "[POST]" url)
            ;; using clj-http directly
            (let [response (clj-http/post url
                                          {
                                           ;; removing content-length, preventing
                                           ;; exception that it's already set
                                           :headers (dissoc
                                                     (:headers request)
                                                     "Content-Length"
                                                     "content-length")
                                           :body (:body request)})]
              {
               :status (:status response)
               :headers (:headers response)
               :body (:body response)}))

          :else
          {:status 405})
         (catch Exception e
           (.printStackTrace e)))))))

#_(-main "localhost" "7078" "7079")
