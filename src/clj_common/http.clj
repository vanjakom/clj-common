(ns clj-common.http
  (:require
   [cats.monad.either :as either]
   [clj-http.client :as clj-http]
   [clj-common.base64 :as base64]
   [clj-common.logging :as logging]))

;; DEPRECATED
;; adds compexity, clj-http.client is easy enough to use directly

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

(defmacro with-basic-auth [user password & body]
  `(binding [*configuration* (update-in
                              *configuration*
                              [:headers]
                              assoc
                              "Authorization"
                              (str
                               "Basic "
                               (base64/string->base64 (str ~user ":" ~password))))]
     ~@body))

(defn get-as-stream
  ([url]
   (get-as-stream url nil))
  ([url configuration]
   (try
    (let [response (clj-http/get url (merge *configuration* configuration))]
      (if (= (:status response) 200)
        (:body response)
        nil))
    (catch Exception e (logging/report-throwable {:url url} e)))))

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
  ([url body-stream] (post-as-stream url {}  body-stream))
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

(defn post-form-as-string
  [url form-map]
  (:body
   (clj-http/post
    url
    {:form-params form-map})))

(defn put-raw-as-stream [url body-stream]
  (try
    (let [response (clj-http/put
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


(defn put-as-stream
  ([url body-stream] (put-as-stream url {}  body-stream))
  ([url configuration body-stream]
   (try
    (let [response (clj-http/put
                    url
                    (assoc
                     (merge
                      *configuration*
                      configuration)
                     :body
                     body-stream))]
      (if (= (:status response) 200)
        (:body response)
        (do
          (logging/report {
                           :fn clj-common.http/put-as-stream
                           :status (:status response)})
          nil)))
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


