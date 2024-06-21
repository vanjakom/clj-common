(ns clj-common.http-proxy
  (:use
   clj-common.clojure)
  (:require
   [clj-http.client :as clj-http]
   [clj-common.as :as as]
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
             (let [response (clj-http/get url
                                          {
                                           ;; removing content-length, preventing
                                           ;; exception that it's already set
                                           :headers (dissoc
                                                     (:headers request)
                                                     "Content-Length"
                                                     "content-length")
                                           :body (:body request)})]
               (println (:headers response))
               {
                :status (:status response)
                :headers (:headers response)
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
                        (str "?" (:query-string request))) )
                 length (as/as-integer
                         (or
                          (get-in request [:headers "Content-Length"])
                          (get-in request [:headers "content-length"])))]
             (println "[POST]" url)
             (println "Content-Length" length)
             ;; using clj-http directly
             (let [response (clj-http/post url
                                           {
                                            ;; removing content-length, preventing
                                            ;; exception that it's already set
                                            :headers
                                            (dissoc
                                             (:headers request)
                                             "Content-Length"
                                             "content-length")
                                            :body (:body request)
                                            ;; fixing issue with apache http failing
                                            ;; when Content-Length is set
                                            :length length})]
               {
                :status (:status response)
                :headers (:headers response)
                :body (:body response)}))

           :else
           {:status 405})
         (catch Exception e
           (.printStackTrace e)))))))

#_(-main  "fcapi-controller-00.lax.sfxcloud.net" "7100" "7100")
