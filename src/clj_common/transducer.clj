(ns clj-common.transducer
  (:require
   [clj-common.io :as io]
   [clj-common.context :as context]))

(defn list-reducing-fn
  "Creates list from qiven sequence"
  ([] (list))
  ([state entry] (conj state entry))
  ([state] state))

(defn list-concat-reducing-fn
  "Concats given lists"
  ([] (list))
  ([state entry] (concat state entry))
  ([state] state))

(defn set-reducing-fn
  "Creates set from given sequence"
  ([] #{})
  ([state entry] (conj state entry))
  ([state] state))

(defn create-map-reducing-fn
  "Creates map from given sequence using key-fn on entry to produce map key"
  [key-fn]
  (fn
    ([] {})
    ([state entry] (assoc state (key-fn entry) entry))
    ([state] state)))

(defn distribution-reducing-fn
  "Creates distribution map per given entry in sequence"
  ([] {})
  ([state entry] (assoc state entry (inc (get state entry 0))))
  ([state] state))

(defn create-line-output-stream-reducing-fn
  "Writes given lines to output stream created by calling output-stream-create-fn.
  Writing line by line of given sequence. Transducer is responsable for closing."
  [output-stream-create-fn]
  (fn
    ([] (output-stream-create-fn))
    ([output-stream entry]
     (io/write-line output-stream entry)
     output-stream)
    ([output-stream] (.close output-stream))))

;;; support for context

(defn create-context-reducing-fn
  "Wraps reducing fn with reporting to context"
  [context reducing-fn]
  (fn
    ([]
     (binding [context/*context* context]
       (context/increment-counter "reduce-setup")
       (reducing-fn)))
    ([state entry]
     (binding [context/*context* context]
       (context/increment-counter "reduce-in")
       (let [result (try
                    (reducing-fn state entry)
                    (catch Exception e
                      (context/increment-counter "reduce-exception")
                      ((:error-fn context) entry e)))]
       (context/increment-counter "reduce-out")
       result)))
    ([state]
     (context/increment-counter "reduce-close")
     (:state state))))

(defn create-context-map-transducer
  "Map transducer with support for context reporting"
  [context map-fn]
  (fn [xf]
    (fn
      ;; init is never called from transduce, Rich's decision
      ;; https://dev.clojure.org/jira/browse/CLJ-1569
      ([]
       (binding [context/*context* context]
         (context/increment-counter "map-setup")
         (xf)))
      ([result input]
       (binding [context/*context* context]
         (context/increment-counter "map-in")
         (let [[success data] (try
                                [true (map-fn input)] 
                                (catch Exception e
                                  (context/increment-counter "map-exception")
                                  (context/error e input)
                                  [false nil]))]
           (if success
             (do
               (context/increment-counter "map-out")
               (xf result data))))))
      ([result]
       (binding [context/*context* context]
         (context/increment-counter "map-close")
         (xf result))))))

(defn create-context-filter-transducer
  "Filter transducer with support for context reporting"
  [context filter-fn]
  (fn [xf]
    (fn
      ([]
       ;; init is never called from transduce, Rich's decision
       ;; https://dev.clojure.org/jira/browse/CLJ-1569
       (binding [context/*context* context]
         (context/increment-counter "filter-setup")
         (xf)))
      ([result input]
       (binding [context/*context* context]
         (context/increment-counter "filter-in")
         (if (try
               (filter-fn input)
               (catch Exception e
                 (context/increment-counter "filter-exception")
                 (context/error e input)
                 false))
           (do
             (context/increment-counter "filter-out")
             (xf result input)))))
      ([result]
       (binding [context/*context* context]
         (context/increment-counter "filter-close")
         (xf result))))))

#_(sequence
   (create-context-filter-transducer (context/create-stdout-context) odd?)
   [1 2 3 4])
#_(sequence
 (comp
  (map inc)
  (create-context-map-transducer (context/create-stdout-context) inc))
 [1 2 3])
#_(transduce
 (create-context-map-transducer (context/create-stdout-context) inc)
 list-reducing-fn
 [1 2 3])







