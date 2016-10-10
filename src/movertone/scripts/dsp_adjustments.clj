(ns movertone.scripts.dsp-adjustments
  (:require [overtone.core :as o]))

(def default-tonic-prominence 0.8)
(def default-non-prominence-reduction 0.8)

(defn nil-plus [n1 n2]
  (if (nil? n1)
    n2
    (+ n1 n2)))

(defn midi-histogram [freqs]
  (frequencies (pmap o/hz->midi freqs)))

(defn normalize-octaves [midi-occurrances]
  (reduce (fn [x [midi occurrances]]
            (update-in x [(mod midi 12)] nil-plus occurrances))
          {}
          midi-occurrances))

(defn find-tonic-midi [freqs prominent-note]
  (let [midi-occurrances (midi-histogram freqs)
        prominent-midi-diff (->>  midi-occurrances
                                  normalize-octaves
                                  (sort-by second >)
                                  ffirst)]
    (cond
      (= :s prominent-note) (+ 60 prominent-midi-diff)
      (= :p prominent-note) (+ 65 prominent-midi-diff)
      :else                 (+ 60 prominent-midi-diff))))

(defn reduce-tonic-prominence [perc hist]
  (let [tonic (ffirst (sort-by second > hist))]
    (update-in hist [tonic] #(* perc %))))

(defn remove-non-prominent-notes [perc hist]
  (->> hist
       (sort-by second >)
       (take (* perc (count hist)))
       (into {})))
