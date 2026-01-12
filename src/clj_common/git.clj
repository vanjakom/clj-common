(ns clj-common.git
  (:require
   [clj-common.context :as context]
   [clj-common.io :as io]
   [clj-common.jvm :as jvm]
   [clj-common.path :as path]))

;; fns to integrate with git, working by forking git process and collecting stdout
;; relies on execute-command in jvm
;; each command returns status map if successful else nil

;; 20260112 supporting context output ( for clj-scheduler )

(defn status
  ([repo-path]
   (status (context/create-stdout-context) repo-path))
  ([context repo-path]
   (let [path (path/path->string repo-path)]
     (context/trace context (str "[GIT][STATUS] " path))
     (let [response (io/input-stream->line-seq
                     (jvm/execute-command path "git status"))]
       (doseq [line response]
         (context/trace context (str "[OUT] " line)))
       (if (some? (first (filter
                          #(.contains % "nothing to commit, working tree clean")
                          response)))
         {:status :clean}
         {:status :diff})))))

(defn pull 
  ([repo-path]
   (pull (context/create-stdout-context) repo-path))
  ([context repo-path]
   (let [path (path/path->string repo-path)]
     (context/trace context (str "[GIT][PULL] " path))
     (let [response (io/input-stream->line-seq
                     (jvm/execute-command-and-check path "git pull"))]
       (doseq [line response]
         (context/trace context (str "[OUT] " line)))
       response))))

#_(status (path/string->path "/Users/vanja/projects/MaplyProject"))
#_(status (path/string->path "/Users/vanja/projects/trek-mate-pins"))
#_(status (path/string->path "/Users/vanja/projects/trek-mate"))

#_(pull (path/string->path "/Users/vanja/projects/trek-mate"))
