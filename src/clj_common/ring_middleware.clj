(ns clj-common.ring-middleware)

(defn wrap-only-query-params-middleware [handler]
  (fn [request]
    (let [encoding (ring.util.request/character-encoding request)]
      (handler
        (ring.middleware.params/assoc-query-params request encoding)))))

(defn post-body-as-string-middleware [handler]
  (fn [request]
    (let [line (slurp (:body request))]
      (println "line: " line)
      (handler
        (assoc
          request
          :body line)))))
