(ns clj-common.ring-middleware)

(require 'ring.util.request)
(require 'ring.middleware.params)

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
