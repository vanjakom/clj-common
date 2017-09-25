(ns clj-common.http-server)

(require 'compojure.core)
(require 'ring.adapter.jetty)
(require 'ring.middleware.json)
(require 'ring.middleware.params)
(require 'ring.middleware.keyword-params)

(def sample-handler
  (compojure.core/routes
    (compojure.core/GET
      "/status"
      _
      (fn [request]
        {:status 200
         :body "ok\n"}))
    (compojure.core/POST
      "/boomerang"
      _
      (fn [request]
        {:status 200
         :body (:body request)}))))

; (def a (clj-common.http-server/create-server 10000 #'clj-common.http-server/sample-handler))

(defn create-server [port handler]
  (ring.adapter.jetty/run-jetty
    handler
    {
      :port port
      :join? false}))

(defn stop-server [server]
  (.stop server))
