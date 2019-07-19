(ns clj-common.http
  (:require [cats.monad.either :as either])
  (:require [clj-http.client :as clj-http])
  (:require [clj-common.logging :as logging]))

(def ^:dynamic *throw-exception* false)
(def ^:dynamic *configuration* {
                                :as :stream
                                :throw-exceptions false})

(defmacro with-user-agent [agent & body]
  `(binding [*configuration* (update-in
                                      *configuration*
                                      [:client-params "http.useragent"] 
                                      (constantly ~agent))]
     ~@body))

(defmacro with-default-user-agent [& body]
  `(with-user-agent "clj-http" ~@body))

(defn get-as-stream [url]
  (try
    (let [response (clj-http/get url *configuration*)]
      (if (= (:status response) 200)
        (:body response)
        nil))
    (catch Exception e (logging/report-throwable {:url url} e))))

(defn get-as-stream-or-error [url]
  (try
    (let [response (clj-http/get url *configuration*)]
      (condp = (:status response)
        200 (either/right (:body response))
        404 (either/right nil)
        (either/left (ex-info "Failing status code" response))))
    (catch Exception e (either/left e))))


(defn get-raw-as-stream
  "Returns status, headers and body ( as stream )"
  [url]
  (try
    (let [response (clj-http/get url *configuration*)]
      (update-in
        response
        [:headers]
        (fn [raw-headers]
          (into
            {}
            (map (fn [[k v]] [(keyword k) v]) raw-headers)))))
    (catch Exception e (logging/report-throwable {:url url} e))))

(def url->response get-raw-as-stream)


(defn post-raw-as-stream [url body-stream]
  (try
    (let [response (clj-http/post
                    url
                    (assoc
                     *configuration*
                     :body
                     body-stream))]
      (update-in
        response
        [:headers]
        (fn [raw-headers]
          (into
            {}
            (map (fn [[k v]] [(keyword k) v]) raw-headers)))))
    (catch Exception e (logging/report-throwable {:url url} e))))

(defn post-as-stream
  ([url body-stream] (post-as-stream url body-stream {}))
  ([url configuration body-stream]
   (try
    (let [response (clj-http/post
                    url
                    (assoc
                     (merge
                      *configuration*
                      configuration)
                     :body
                     body-stream))]
      (if (= (:status response) 200)
        (:body response)
        nil))
    (catch Exception e (logging/report-throwable {:url url} e)))))

(defn parse-query-string [uri]
  (let [query-string (second (.split uri "\\?"))
        params (into
                 {}
                 (map
                   (fn [param]
                     (let [pair (.split param "=")]
                       [(keyword (first pair)) (second pair)]))
                   (.split query-string "&")))]
    params))


