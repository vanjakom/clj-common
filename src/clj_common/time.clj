(ns clj-common.time)

(require 'clj-time.coerce)

(defn timestamp []
  (System/currentTimeMillis))

(defn timestamp-second []
  (int (/ (System/currentTimeMillis) 1000)))

(defn timestamp-of-day-second
  "Returns timestamp of begging of current day, useful for reports"
  []
  (let [current-timestamp (timestamp-second)]
    (*
      (int (/
             current-timestamp
             (* 24 60 60)))
      (* 24 60 60))))


(comment
  (timestamp-second)
  (timestamp-of-day-second))


(defn timestamp->plain-date [timestamp]
  (if (< timestamp 1000000000000)
    (clj-time.coerce/from-long (* timestamp 1000))
    (clj-time.coerce/from-long timestamp)))

(defn timestamp->date [timestamp]
  	(str (.toString
		(if (< timestamp 1000000000000)
			(clj-time.coerce/from-long (* timestamp 1000))
			(clj-time.coerce/from-long timestamp))) " GMT"))
(def timestamp-date  timestamp->date)

(def ^:dynamic *date-format* "yyyy-MM-dd HH:mm:ss")
(defn timestamp->date-in-timezone [timezone timestamp]
  (let [dateTimeFormatter (new java.text.SimpleDateFormat *date-format*)]
    (.setTimeZone
     dateTimeFormatter
     (java.util.TimeZone/getTimeZone "Europe/Belgrade"))
    (.format
     dateTimeFormatter
     (if (< timestamp 1000000000000) (* timestamp 1000) timestamp))))

(defn date->timestamp [date]
  (let [date-time-formatter (new java.text.SimpleDateFormat *date-format*)]
    (.getTime
     (.parse
      date-time-formatter
      date))))

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
