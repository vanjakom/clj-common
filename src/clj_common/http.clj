(ns clj-common.http)

(require '[clj-http.client :as clj-http])

(defn get-as-stream [url]
  (let [response (clj-http/get url {:as :stream :throw-exceptions false})]
    (if (= (:status response) 200)
      (:body response)
      nil)))
