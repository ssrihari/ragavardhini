(ns ^{:doc "Reads, parses ragams from file, and makes them accessible"}
  movertone.ragams
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [medley.core :as m]
            [movertone.db :as db]))

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

(defn assoc-mela-number [mela [janyam-name janyam-info]]
  [janyam-name (assoc janyam-info
                 :name janyam-name
                 :parent-mela-name (:name mela)
                 :parent-mela-num (:num mela))])

(defn assoc-mela-numbers [[mela janyams]]
  [mela (into {} (map (partial assoc-mela-number mela)
                      janyams))])

(def melakarthas
  (->> janyams-by-melakarthas
       keys
       (map name->melakartha)
       (into {})))

(def janyams
  (->> janyams-by-melakarthas
       (map assoc-mela-numbers)
       (into {})
       vals
       (apply merge-with deduplicate-by-priority)))

(def ragams
  (merge janyams melakarthas))

(defn ragams-with-duplicates []
  (->> janyams-by-melakarthas
       vals
       (apply concat)
       (group-by first)
       (m/map-vals count)
       (filter #(> (second %) 1))
       (sort-by second )))

(defn db-ragam->ragam [db-ragam]
  (ragams (keyword (:name db-ragam))))

(defn build-result [results perc]
  (let [result-ragams (mapv db-ragam->ragam results)]
    {:ragam (first result-ragams)
     :more (rest result-ragams)
     :perc (format "%.1f" perc)}))

(defn search
  "Search in postgres using trigram similary using an index, and
  order results by soundex difference. perc is the trigram similarity that
  decreases from 0.9 to 0.0 to find closest results. This might be slow
  and might need optimizations later."
  ([ragam] (search ragam 0.4))
  ([ragam perc]
     (when (pos? perc)
       (let [result-ragams (db/search ragam perc)]
         (if (empty? result-ragams)
           (search ragam (- perc 0.1))
           (build-result result-ragams perc))))))
