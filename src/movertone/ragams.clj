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

(def raga-to-kritis
  (read-file "raga-to-kritis.edn"))

(def raga-to-kritis-more
  (read-file "raga-to-kritis-more.edn"))

(def all-kritis
  (concat
   (mapcat (fn [[r ks]] (map #(assoc % :ragam r) ks)) raga-to-kritis)
   (mapcat (fn [[r ks]] (map #(assoc % :ragam r) ks)) raga-to-kritis-more)))

(def all-kritis-by-name
  (zipmap (map :kriti all-kritis)
          all-kritis))

(defn ragams-with-duplicates []
  (->> janyams-by-melakarthas
       vals
       (apply concat)
       (group-by first)
       (m/map-vals count)
       (filter #(> (second %) 1))
       (sort-by second )))
