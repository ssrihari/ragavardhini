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
       (remove s/blank?)
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

(defn one-swaram-probabilities-for-file [file {:keys [tonic-prominence] :as opts}]
  (let [freqs (freqs-from-file file)]
    (->> (freqs->swarams freqs)
         frequencies
         (adj/reduce-tonic-prominence tonic-prominence))))

(defn one-swaram-probabilities [files & {:keys [npr-factor] :as opts}]
  (->> (pmap #(one-swaram-probabilities-for-file % opts) files)
       (apply merge-with +)
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

(defn one-swaram->two-swaram-mapping [freqs]
  (->> (two-swaram-freqs freqs)
       (group-by (fn [[[fs ss] fr]] fs))
       (pmap (fn [[sw t-sh]]
               [sw (m/map-keys second (into {} t-sh))]))
       (into {})))

(defn two-swaram-histogram-for-file [file]
  (->> (freqs-from-file file)
       one-swaram->two-swaram-mapping
       (m/map-vals #(adj/reduce-tonic-prominence 0.2 %))))

(defn two-swaram-probabilities [files & opts]
  (let [osw (apply one-swaram-probabilities files opts)]
    (def *osw osw)
    (->> files
         (pmap two-swaram-histogram-for-file)
         (apply merge-with #(merge-with + %1 %2))
         (#(select-keys % (keys osw)))
         (m/map-vals #(select-keys % (keys osw)))
         (m/map-vals ->perc-histogram))))
