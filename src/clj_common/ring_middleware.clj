(ns clj-common.ring-middleware)

(require 'ring.util.request)
(require 'ring.middleware.params)
(require 'ring.middleware.file)
(require '[clj-common.path :as path])

(require '[clj-common.json :as json])

(defn wrap-only-query-params-middleware [handler]
  (fn [request]
    (let [encoding (ring.util.request/character-encoding request)]
      (handler
        (ring.middleware.params/assoc-query-params request encoding)))))

(defn post-body-as-string-middleware [handler]
  (fn [request]
    (let [line (slurp (:body request))]
      (handler
        (assoc
          request
          :body line)))))

(defn post-body-as-json-middleware [handler]
  (fn [request]
    (let [line (slurp (:body request))]
      (handler
        (assoc
          request
          :body (json/read-keyworded line))))))

(defn static-file-middleware
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

