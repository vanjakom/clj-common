(ns clj-common.ring-middleware)

(require 'ring.util.request)
(require 'ring.middleware.params)
(require 'ring.middleware.file)
(require '[clj-common.path :as path])

(require '[clj-common.json :as json])
(require '[clj-common.logging :as logging])

(defn wrap-only-query-params [handler]
  (fn [request]
    (let [encoding (ring.util.request/character-encoding request)]
      (handler
        (ring.middleware.params/assoc-query-params request encoding)))))
(def wrap-only-query-params-middleware wrap-only-query-params)

(defn post-body-as-string [handler]
  (fn [request]
    (let [line (slurp (:body request))]
      (handler
        (assoc
          request
          :body line)))))
(def post-body-as-string-middleware post-body-as-string)

(defn post-body-as-json [handler]
  (fn [request]
    (let [line (slurp (:body request))]
      (handler
        (assoc
          request
          :body (json/read-keyworded line))))))
(def post-body-as-json-middleware post-body-as-json)

(defn static-file
  "Serves static files from root-path by looking rest of uri when prefix is removed,
  useful when same server will serve both api and web, /api/stats and /web/stats"
  [prefix root-path]
  (fn [request]
    (if-let [response (ring.middleware.file/file-request
                        (assoc
                          request
                          :uri
                          (.substring (:uri request) (.length prefix)))
                        (path/path->string root-path))]
      response
      {:status 404})))
(def static-file-middleware static-file)

(defn wrap-exception-to-logging [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable t
        (logging/report-throwable (select-keys request [:uri]) t)
        {:status 500}))))


(defn expose-variable [handler]
  (fn [request]


    ))
