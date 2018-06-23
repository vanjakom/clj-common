(ns clj-common.async
  (:require
    [clojure.core.async :as async]
    clojure.core.async.impl.protocols))

(defn closed? [channel]
  (clojure.core.async.impl.protocols/closed? channel))
