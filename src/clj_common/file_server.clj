(ns clj-common.file-server
  (:require
   [clj-common.localfs :as fs]
   [clj-common.io :as io]
   [clj-common.path :as path]
   [clj-common.http-server :as server]
   compojure.core))

(def mapping (atom {}))

(defn register-mapping [namespace handler]
  (swap! mapping #(update % namespace (constantly handler))))

#_(register-mapping
 "tile-ravni-roads"
 (fn [path]
   (let [segments (.split path "/")
         zoom (nth segments 0)
         x (nth segments 1)
         y (nth segments 2)
         path (path/child
               ["Users" "vanja" "dataset-cloud" "ravni" "tile-cache"
                "output-roads"]
               (str zoom "_" x "_" y ".png"))]
     (println "[tile-ravni]" zoom x y path)
     (with-open [is (fs/input-stream path )]
       (let [content (io/input-stream->bytes is)]
         {
          :status 200
          :body (io/bytes->input-stream content)})))))

#_(deref mapping)

(def handler
  (compojure.core/routes
   (compojure.core/GET
    "/status"
    _
    {:status 200 :body "ok\n"})
   (compojure.core/GET
    "/static/:namespace/*"
    request
    (let [namespace (get-in request [:params :namespace])
          path (get-in request [:params :*])]
      (println "[request]" namespace path)
      ((get (deref mapping) namespace) path)))))

(server/create-server 7080 handler)
