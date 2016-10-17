(ns movertone.scripts.frequencies
  (:require [clojure.string :as s]
            [clojure.pprint :refer [pprint]]
            [medley.core :as m]
            [overtone.core :as o]
            [movertone.swarams :as sw]
            [movertone.scripts.dsp-adjustments :as adj]
            [movertone.scripts.samples :as samples])
  (:import [java.util HashMap]))

(defn with-dir-name [filename]
  (str "resources/kosha/" filename))

(defn freqs-from-file [file]
  (->> (s/split (slurp (with-dir-name file)) #"\n")
       (remove #{"0"})
       (remove s/blank?)
       (map #(Double/parseDouble %))))

(defn pprint-histogram [hist]
  (pprint (sort-by second > hist)))

(defn ->perc-histogram [hist]
  (let [total (reduce + (vals hist))]
    (m/map-vals #(* 100.0 (/ % total))
                hist)))

(defn hz->swaram [tonic-midi hz]
  (sw/midi->swaram (o/hz->midi hz) tonic-midi))

(defn freqs->swarams [freqs & [prominent-note]]
  (let [tonic-midi (adj/find-tonic-midi freqs prominent-note)]
    (map #(hz->swaram tonic-midi %) freqs)))

(defn one-swaram-probabilities-for-file [file {:keys [tonic-prominence] :as opts}]
  (let [freqs (freqs-from-file file)]
    (->> (freqs->swarams freqs)
         frequencies
         (adj/reduce-tonic-prominence tonic-prominence))))

(defn one-swaram-probabilities [files & {:keys [npr-factor] :as opts}]
  (->> (pmap #(one-swaram-probabilities-for-file % opts) files)
       (apply merge-with +)
       (#(dissoc % nil))
       (adj/remove-non-prominent-notes npr-factor)
       ->perc-histogram))

(defn two-swaram-freqs [freqs]
  (let [swarams (freqs->swarams freqs)]
    (frequencies
     (loop [acc []
            [f s :as all] swarams]
       (if s
         (recur (conj acc [f s]) (rest all))
         acc)))))

(defn three-swaram-freqs [freqs]
  (let [swarams (freqs->swarams freqs)]
    (frequencies
     (loop [acc []
            [f s t :as all] swarams]
       (if t
         (recur (conj acc [f s t]) (rest all))
         acc)))))

(defn n-swaram-freqs [freqs n]
  (case n
    2 (two-swaram-freqs freqs)
    3 (three-swaram-freqs freqs)))

(defn one-swaram->two-swaram-mapping [freqs]
  (->> (n-swaram-freqs freqs 2)
       (group-by (fn [[[fs ss] fr]] fs))
       (map (fn [[sw t-sh]]
              [sw (m/map-keys second (into {} t-sh))]))
       (into {})))

(defn two-swaram-histogram-for-file [file]
  (->> (freqs-from-file file)
       one-swaram->two-swaram-mapping
       (m/map-vals #(adj/reduce-tonic-prominence 0.2 %))))

(defn two-swaram-probabilities [files & opts]
  (let [osw (apply one-swaram-probabilities files opts)
        absolute-two (->> files
                          (pmap two-swaram-histogram-for-file)
                          (apply merge-with #(merge-with + %1 %2))
                          (#(select-keys % (keys osw)))
                          (m/map-vals #(select-keys % (keys osw))))]
    {:one      osw
     :flat-two (->> absolute-two
                    (map (fn [[sw swh]] (m/map-keys (fn [ns] [sw ns]) swh)))
                    (into {}))
     :two      (->> absolute-two
                    (m/map-vals ->perc-histogram))}))

(defn two-swaram->three-swaram-mapping [freqs]
  (->> (n-swaram-freqs freqs 3)
       (group-by (fn [[[fs ss ts] fr]] [fs ss]))
       (map (fn [[sw t-sh]]
              [sw (m/map-keys #(nth % 2) (into {} t-sh))]))
       (into {})))

(defn three-swaram-histogram-for-file [file]
  (->> (freqs-from-file file)
       two-swaram->three-swaram-mapping
       (m/map-vals #(adj/reduce-tonic-prominence 0.2 %))))

(defn three-swaram-probabilities [files & opts]
  (let [{:keys [one two] :as one-and-two} (apply two-swaram-probabilities files opts)
        prom-sws (set (keys one))]
    (merge one-and-two
           {:three
            (->> files
                 (pmap three-swaram-histogram-for-file)
                 (apply merge-with #(merge-with + %1 %2))
                 (filter (fn [[[f s]]] (and (prom-sws f)
                                            (prom-sws s))))
                 (into {})
                 (m/map-vals #(select-keys % (keys one)))
                 (m/map-vals ->perc-histogram))})))
