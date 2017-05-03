(ns clj-common.http-test)

(require '[clj-common.time :as time])
(require '[clj-common.metrics :as metrics])

; usage
;(clj-common.http-test/test-route
;  (fn []
;    (fn []
;      (clj-common.io/input-stream2string (clj-common.http/get-as-stream "http://mungolab.com"))))
;  (fn [response] (.contains response "</html>"))
;  10
;  60)

(defn test-route [request-create-fn response-valid-fn number-of-threads sum-requests]
  (let [per-thread-request (int (/ sum-requests number-of-threads))
        thread-pool (java.util.concurrent.Executors/newFixedThreadPool number-of-threads)
        main-fn (fn [thread-name]
                  (.setName (Thread/currentThread) thread-name)
                  (doseq [iteration (range 0 per-thread-request)]
                    (let [request-fn (request-create-fn)
                          [duration response] (time/timed-response-fn (request-fn))]
                      (if (response-valid-fn response)
                        (do
                          (metrics/inc-counter "response.vaild")
                          (metrics/report-timer "response.duration" duration)
                          (metrics/report-timer (str "per-thread." thread-name ".duration") duration))
                        (metrics/inc-counter "response.invaild")))))]
    (doseq [thread-num (range 0 number-of-threads)]
      (let [thread-fn (partial main-fn (str "worker-" thread-num))]
        (.execute thread-pool thread-fn)))
    (.shutdown thread-pool)
    (.awaitTermination thread-pool 30 java.util.concurrent.TimeUnit/DAYS)
    (if (not (.isTerminated thread-pool))
      (throw (new Exception "unable to await executor termination")))
    (println "testing finished")))
