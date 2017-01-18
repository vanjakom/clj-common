(ns clj-common.time)

(require 'clj-time.coerce)

(defn timestamp []
  (System/currentTimeMillis))

(defn timestamp-date [timestamp]
  	(str (.toString
		(if (< timestamp 1000000000000)
			(clj-time.coerce/from-long (* timestamp 1000))
			(clj-time.coerce/from-long timestamp))) " GMT"))
