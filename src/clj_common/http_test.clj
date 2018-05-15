(ns clj-common.http-test)

(require '[clj-common.time :as time])
(require '[clj-common.metrics :as metrics])
(require '[clj-common.logging :as logging])

; usage
;(clj-common.http-test/test-route
;  (fn []
;    (fn []
;      (clj-common.io/input-stream2string (clj-common.http/get-as-stream "http://mungolab.com"))))
;  (fn [response] (.contains response "</html>"))
;  10
;  60)

(def ^:dynamic *report-duration-per-each* false)

(defn test-route
  ([request-create-fn response-valid-fn number-of-threads sum-requests sleep-between-requests metrics]
   (let [per-thread-request (int (/ sum-requests number-of-threads))
         thread-pool (java.util.concurrent.Executors/newFixedThreadPool number-of-threads)
         report-duration-per-each *report-duration-per-each*
         main-fn (fn [thread-name]
                   (binding [clj-common.metrics/*metrics* metrics
                             *report-duration-per-each* report-duration-per-each]
                     (.setName (Thread/currentThread) thread-name)
                     (doseq [iteration (range 0 per-thread-request)]
                       (let [[id request-fn] (request-create-fn)
                             [duration response] (time/timed-response-fn (request-fn))]
                         (if *report-duration-per-each*
                           (logging/report {
                                             :duration duration
                                             :request id }))
                         (if (response-valid-fn response)
                           (do
                             (metrics/inc-counter "response.vaild")
                             (metrics/report-timer "response.duration" duration)
                             (metrics/report-timer (str "per-thread." thread-name ".duration") duration))
                           (do
                             (logging/report response)
                             (metrics/inc-counter "response.invaild")))
                         (if (> (or sleep-between-requests 0) 0)
                           (Thread/sleep sleep-between-requests))))))]
     (doseq [thread-num (range 0 number-of-threads)]
       (let [thread-fn (partial main-fn (str "worker-" thread-num))]
         (.execute thread-pool thread-fn)))
     (.shutdown thread-pool)
     (.awaitTermination thread-pool 30 java.util.concurrent.TimeUnit/DAYS)
     (if (not (.isTerminated thread-pool))
       (throw (new Exception "unable to await executor termination")))
     (println "testing finished")))
  ([request-create-fn response-valid-fn number-of-threads sum-requests metrics]
   (test-route request-create-fn response-valid-fn number-of-threads sum-requests 0 metrics)))
