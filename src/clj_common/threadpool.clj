(ns clj-common.threadpool)

(defn create-fixed-executor [threads-number]
  (java.util.concurrent.Executors/newFixedThreadPool threads-number))

(defn execute-async [pool fn-to-execute]
  (.submit
      ^java.util.concurrent.ExecutorService pool
      ^Callable fn-to-execute))
