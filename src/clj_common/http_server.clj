(ns clj-common.http-server
  (:require
    compojure.core
    ring.adapter.jetty
    ring.middleware.json
    ring.middleware.params
    ring.middleware.keyword-params

    [clj-common.logging :as logging]
    [clj-common.ring-middleware :as ring-middleware]))


;; defonce, not def: reloading this namespace with a plain def resets the map
;; and orphans whatever is already running - create-server then finds no old
;; handle, never stops it, and the old jetty keeps its socket. that is easy to
;; miss because the new server does start, so it looks like the reload worked
(defonce servers (atom {}))

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
#_(clj-common.http-server/create-server
 10000
 #'clj-common.http-server/sample-handler)


(declare stop-server)

;; servers started here are desktop tools, meant to be reached from this
;; machine only. ring binds every interface when :host is left unset, which
;; puts them on the local network: on a cafe or conference wifi anyone able
;; to reach the machine can read whatever they serve, and there is no
;; authentication anywhere in this stack
;;
;; pass {:host "0.0.0.0"} deliberately for the rare server that has to answer
;; another device, a phone on the same wifi for instance
(def default-host "127.0.0.1")

(defn create-server
  ([port handler]
   (create-server port handler nil))
  ([port handler {:keys [host] :or {host default-host}}]
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
                           :host host
                           :port port
                           :join? false})]
     (logging/report {:fn 'clj-common.http-server/create-server
                      :port port
                      :host host
                      :message "started server"})
     (swap! servers assoc port server-handle)
     server-handle)))

(defn create-https-server
  ([port keystore-file-name keystore-password handler]
   (create-https-server port keystore-file-name keystore-password handler nil))
  ([port keystore-file-name keystore-password handler
    {:keys [host] :or {host default-host}}]
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
                          :host host
                          :ssl-port port
                          :keystore keystore-file-name
                          :key-password keystore-password
                          :truststore keystore-file-name
                          :trust-password keystore-password
                          :join? false})]
     (logging/report {:fn 'clj-common.http-server/create-server
                      :port port
                      :host host
                      :message "started server"})
     (swap! servers assoc port server-handle)
     server-handle)))

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
