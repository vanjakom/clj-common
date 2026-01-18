(ns clj-common.git
  (:require
   [clj-common.io :as io]
   [clj-common.jvm :as jvm]
   [clj-common.path :as path]))

;; fns to integrate with git, working by forking git process and collecting stdout
;; relies on execute-command in jvm
;; each command returns status map if successful else nil

(defn status [repo-path]
  (let [path (path/path->string repo-path)]
    (println "[STATUS]" path)
    (let [response (io/input-stream->line-seq
                    (jvm/execute-command path "git status"))]
      (println response)
      (cond
        (some? (first (filter
                         #(.contains % "nothing to commit, working tree clean")
                         response)))
        {:status :clean}
        (some? (first (filter
                       #(.contains % "fatal: not a git repository (or any of the parent directories): .git")
                       response)))
        {:status :not-git}
        
        :else
        {:status :diff}))))

#_(status (path/string->path "/Users/vanja/projects/MaplyProject"))
#_(status (path/string->path "/Users/vanja/projects/trek-mate-pins"))
#_(status (path/string->path "/Users/vanja/projects/trek-mate"))
#_(status (path/string->path "/Users/vanja/projects/classloader-playground"))

#_(io/input-stream->std-out
 (let [path "/Users/vanja/projects/classloader-playground"]
   (jvm/execute-command path "git status")))
