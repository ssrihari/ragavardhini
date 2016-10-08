(ns movertone.scripts.frequencies
  (:require [clojure.string :as s]
            [clojure.pprint :refer [pprint]]
            [medley.core :as m]
            [overtone.core :as o]
            [movertone.swarams :as sw]
            [movertone.scripts.dsp-adjustments :as adj]
            [movertone.scripts.samples :as samples]))

(defn with-dir-name [filename]
  (str "resources/kosha/" filename))

(defn freqs-from-file [file]
  (->> (s/split (slurp (with-dir-name file)) #"\n")
       (remove #{"0"})
       (pmap #(Double/parseDouble %))))

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
    (pmap #(hz->swaram tonic-midi %) freqs)))

(defn one-swaram-probabilities-for-file [{:keys [f prominent-note] :as file}]
  (let [freqs (freqs-from-file f)]
    (->> (freqs->swarams freqs prominent-note)
         frequencies
         (adj/reduce-tonic-prominence 0.5))))

(defn one-swaram-probabilities [files]
  (->> (pmap one-swaram-probabilities-for-file files)
       (apply merge-with +)
       (adj/remove-non-prominent-notes 0.25)
       ->perc-histogram))

(defn two-swaram-freqs [freqs]
  (let [swarams (freqs->swarams freqs)]
    (frequencies
     (loop [acc []
            [f s :as all] swarams]
       (if s
         (recur (conj acc [f s]) (rest all))
         acc)))))

(defn one-swaram->two-swaram-mapping [freqs]
  (->> (two-swaram-freqs freqs)
       (group-by (fn [[[fs ss] fr]] fs))
       (pmap (fn [[sw t-sh]]
               [sw (m/map-keys second (into {} t-sh))]))
       (into {})))

(defn two-swaram-histogram-for-file [file]
  (->> (freqs-from-file file)
       one-swaram->two-swaram-mapping
       (m/map-vals #(adj/reduce-tonic-prominence 0.6 %))))

(defn two-swaram-probabilities [files]
  (let [osw (one-swaram-probabilities (map (fn [f] {:f f}) files))]
    (def *osw osw)
    (->> files
         (pmap two-swaram-histogram-for-file)
         (apply merge-with #(merge-with + %1 %2))
         (#(select-keys % (keys osw)))
         (m/map-vals #(select-keys % (keys osw)))
         (m/map-vals ->perc-histogram))))

(comment
  (time (doall
         (->> samples/mohanam-files
              (map (fn [f] {:f f}))
              one-swaram-probabilities
              pprint-histogram))))
