(ns clj-common.http-server)

(require 'compojure.core)
(require 'ring.adapter.jetty)
(require 'ring.middleware.json)
(require 'ring.middleware.params)
(require 'ring.middleware.keyword-params)

(require '[clj-common.ring-middleware :as ring-middleware])


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


; to test go to http://localhost:7051/static/test.html
; will retreive test.html from /Users/vanja/test-http
; (def
;  handle
;  (clj-common.http-server/create-server
;    7051
;    (clj-common.http-server/create-static-file-handler
;      "static"
;      ["Users" "vanja" "test-http"])))

(defn create-static-file-handler [prefix root-path]
  (compojure.core/GET
      (str "/" prefix "/*")
      _
      (ring-middleware/static-file-middleware
        prefix
        root-path)))

; stats http server with sample handler on 10000
;(def
;  a
;  (clj-common.http-server/create-server
;    10000
;    #'clj-common.http-server/sample-handler))

(defn create-server [port handler]
  (ring.adapter.jetty/run-jetty
    handler
    {
      :port port
      :join? false}))

(defn stop-server [server]
  (.stop server))
