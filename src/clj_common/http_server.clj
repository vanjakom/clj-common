(ns clj-common.http-server
  (:require
    compojure.core
    ring.adapter.jetty
    ring.middleware.json
    ring.middleware.params
    ring.middleware.keyword-params

    [clj-common.logging :as logging]
    [clj-common.ring-middleware :as ring-middleware]))


(def servers (atom {}))

(def sample-handler
  (compojure.core/routes
    (compojure.core/GET
      "/status"
      _
      (fn [request]
        {:status 200
         :body "ok\n"}))
    (compojure.core/GET
      "/exception"
      _
      (fn [request]
        (throw (new Exception "On purpose"))))
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

(declare stop-server)


(defn create-server [port handler]
  (logging/report "new code")
  (if-let [old-handle (get @servers port)]
    (do
      (logging/report {:fn 'clj-common.http-server/create-server
                       :port port
                       :message "stopped old server"})
      (stop-server old-handle)))

  (let [server-handle (ring.adapter.jetty/run-jetty
                        handler
                        {
                          :port port
                          :join? false})]
    (logging/report {:fn 'clj-common.http-server/create-server
                     :port port
                     :message "started server"})
    (swap! servers assoc port server-handle)
    server-handle))

(defn create-https-server [port keystore-file-name keystore-password handler]
  (logging/report "new code")
  (if-let [old-handle (get @servers port)]
    (do
      (logging/report {:fn 'clj-common.http-server/create-server
                       :port port
                       :message "stopped old server"})
      (stop-server old-handle)))

  (let [server-handle (ring.adapter.jetty/run-jetty
                        handler
                        {
                         :ssl true
                         :ssl-port port
                         :keystore keystore-file-name
                         :key-password keystore-password
                         :truststore keystore-file-name
                         :trust-password keystore-password
                          :join? false})]
    (logging/report {:fn 'clj-common.http-server/create-server
                     :port port
                     :message "started server"})
    (swap! servers assoc port server-handle)
    server-handle))

(comment
  (create-server 7090 sample-handler)
)

(defn server-port [handle]
  (.getPort (first (.getConnectors handle))))

(defmulti stop-server (fn [argument]
                           (cond
                             (instance? java.lang.Number argument) :port
                             :else :handle)))

(defmethod stop-server :port [port]
  (if-let [handle (get @servers port)]
    (do
      (.stop handle)
      (swap! servers dissoc port))))

(defmethod stop-server :handle [handle]
  (let [port (server-port handle)]
    (.stop handle)
    (swap! servers dissoc port)))

(defn active-ports []
  (keys @servers))
