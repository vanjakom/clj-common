(ns clj-common.maven
  (:require
    [clj-common.http-server :as http]
    [clj-common.path :as path]
    [clj-common.jvm :as jvm]))

(defn create-local-maven-repository [port path]
  (http/create-server
    port
    (http/create-static-file-handler "maven" path)))




(comment
  (create-local-maven-repository
    8080
    (path/child
      (jvm/home-path)
      ".m2" "repository"))


  (http/active-ports)


  (keys @clj-common.http-server/servers)


  (.getPort (first (.getConnectors (first (vals @clj-common.http-server/servers))))))
