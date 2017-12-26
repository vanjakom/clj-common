(ns clj-common.threadpool)

(require '[clj-common.metrics :as metrics])
(require '[clj-common.logging :as logging])

(import java.util.concurrent.ExecutorService)
(import java.util.concurrent.Executors)
(import java.util.concurrent.TimeUnit)

(defn create-fixed-executor [threads-number]
  (Executors/newFixedThreadPool threads-number))

(defn wait-and-terminate [pool]
  (.shutdown pool)
  (.awaitTermination pool 60 TimeUnit/MINUTES))


(defn execute-wrapper [fn-to-execute]
  (let [metrics metrics/*metrics*]
    (fn []
      (binding [metrics/*metrics* metrics]
        (try
          (fn-to-execute)
          (catch Throwable e
            (let [thread-name (.getName (Thread/currentThread))]
              (logging/report-throwable {:thread thread-name} e)
              (metrics/inc-counter
                (str "threadpool." thread-name ".exception")))))))))

(defn execute-async [pool fn-to-execute]
  (.submit
    ^ExecutorService pool
    ^Callable (execute-wrapper fn-to-execute)))

