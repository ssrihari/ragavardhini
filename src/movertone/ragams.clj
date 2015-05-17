(ns ^{:doc "Reads, parses ragams from file, and makes them accessible"}
  movertone.ragams
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [medley.core :as m]
            [clojure.pprint :as pp]))

(defn read-file [filename]
  (-> filename io/resource slurp edn/read-string))

(defn write-file [filename data]
  (spit (-> filename io/resource)
        (-> data pp/pprint with-out-str)))

(defn deduplicate-by-priority [old-val new-val]
  (if (< (:alt-priority old-val 0)
         (:alt-priority new-val 1))
    old-val
    new-val))

(defn name->melakartha [{:keys [name] :as melakartha}]
  [name melakartha])

(def ragas-file
  "ragas.edn")

(def janyams-by-melakarthas
  (read-file ragas-file))

(def melakarthas
  (->> janyams-by-melakarthas
       keys
       (map name->melakartha)
       (into {})))

(def janyams
  (->> janyams-by-melakarthas
       vals
       (apply merge-with deduplicate-by-priority)))

(def ragams
  (merge melakarthas janyams))

(defn ragams-with-duplicates []
  (->> janyams-by-melakarthas
       vals
       (apply concat)
       (group-by first)
       (m/map-vals count)
       (filter #(> (second %) 1))
       (sort-by second )))
