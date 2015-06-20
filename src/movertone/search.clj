(ns movertone.search
  (:require [movertone.db :as db]
            [movertone.ragams :as r]))

(defn db-ragam->ragam [db-ragam]
  (r/ragams (keyword (:name db-ragam))))

(defn db-kriti->kriti [db-kriti]
  (r/all-kritis-by-name (:name db-kriti)))

(defn build-ragam-result [results perc]
  (let [result-ragams (mapv db-ragam->ragam results)
        best-result (first result-ragams)]
    {:ragam best-result
     :more-ragams (rest result-ragams)
     :perc (format "%.1f" perc)
     :kritis (concat (get r/raga-to-kritis (:name best-result))
                     (get r/raga-to-kritis-more (:name best-result)))}))

(defn build-kriti-result [results]
  (let [result-kritis (mapv db-kriti->kriti results)
        best-result (first result-kritis)]
    {:kriti best-result
     :more-kritis (rest result-kritis)}))

(defn search-ragam
  "Search in postgres using trigram similary using an index, and
  order results by soundex difference. perc is the trigram similarity that
  decreases from 0.9 to 0.0 to find closest results. This might be slow
  and might need optimizations later."
  ([ragam] (search-ragam ragam 0.3))
  ([ragam perc]
     (when (pos? perc)
       (let [result-ragams (db/search-ragam ragam perc)]
         (if (empty? result-ragams)
           (search-ragam ragam (- perc 0.1))
           (build-ragam-result result-ragams perc))))))

(defn search-kriti [kriti-name]
  (build-kriti-result (db/search-kriti kriti-name)))
