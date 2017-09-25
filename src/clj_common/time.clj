(ns clj-common.time)

(require 'clj-time.coerce)

(defn timestamp []
  (System/currentTimeMillis))

(defn timestamp-date [timestamp]
  	(str (.toString
		(if (< timestamp 1000000000000)
			(clj-time.coerce/from-long (* timestamp 1000))
			(clj-time.coerce/from-long timestamp))) " GMT"))

(defn millis->seconds [timestamp]
  (long (/ timestamp 1000)))

(defn seconds->millis [timestamp]
  (* timestamp 1000))

(defmacro timed-fn [name & exprs]
  `(let [start# (System/nanoTime)]
    (let [result# ~@exprs
          duration# (/ (- (System/nanoTime) start#) 1000000.0)]
      (println ~name " duration: " duration# "ms")
      result#)))

(defmacro timed-response-fn [& exprs]
  `(let [start# (System/nanoTime)]
    (let [result# ~@exprs
          duration# (/ (- (System/nanoTime) start#) 1000000.0)]
      [duration# result#])))
