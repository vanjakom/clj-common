(ns clj-common.http)

(require '[clj-http.client :as clj-http])

(defn get-as-stream [url]
  (:body (clj-http/get url {:as :stream})))
