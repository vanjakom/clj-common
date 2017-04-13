(ns clj-common.jvm)

(defn get-memory []
  (let [runtime (Runtime/getRuntime)
        free-memory (.freeMemory runtime)
        max-memory (.maxMemory runtime)
        total-memory (.totalMemory runtime)]
    {
      :free-memory free-memory
      :max-memory max-memory
      :total-memory total-memory}))

(defn print-memory []
  (let [{
          free-memory :free-memory
          max-memory :max-memory
          total-memory :total-memory}
        (get-memory)]
    (println
      "Memory: "
      (int (/ free-memory 1024 1024)) "MB free, "
      (int (/ max-memory 1024 1024)) "MB max, "
      (int (/ total-memory 1024 1024)) "MB total")))

