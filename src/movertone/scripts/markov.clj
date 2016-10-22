(ns movertone.scripts.markov
  (:require [movertone.scripts.frequencies :as f]
            [movertone.scripts.samples :as samples]
            [markov-chains.core :as mc]
            [medley.core :as m]
            [movertone.scripts.dsp-adjustments :as adj]
            [movertone.scripts.demo :as demo]
            [movertone.playback :as playback]))

(defn rs []
  (overtone.core/recording-stop))

(defn rstart [raga-name nth-order gathi]
  (overtone.core/recording-start (str "resources/generated/" (gensym "gen-")
                                      "-raga-" (name raga-name)
                                      "-nth-order-" nth-order
                                      "-gathi-" gathi
                                      ".wav")))

(defn make-adjustments [tonic-prominence npr-factor]
  (adj/reduce-tonic-prominence tonic-prominence)
  (adj/remove-non-prominent-notes npr-factor))

(defn prominent-notes [hist perc]
  (->> hist
       (sort-by second >)
       (take (* perc (count hist)))
       keys
       set))

(defn swarams [file]
  (->> file
       f/freqs-from-file
       f/freqs->swarams
       (remove nil?)))

(defn swarams-for-raga [raga-files npr-factor]
  (let [raw-swarams (mapcat swarams raga-files)
        hist (frequencies raw-swarams)
        prominent-notes (prominent-notes hist npr-factor)]
    (filter prominent-notes raw-swarams)))

(defonce collations (atom {}))

(defn get-or-add-collation [raga-name swarams nth-order]
  (or (get-in @collations [raga-name nth-order])
      (do (swap! collations assoc-in [raga-name nth-order] (mc/collate swarams nth-order))
          (get-or-add-collation raga-name swarams nth-order))))

(defn play-collated [raga-name swarams nth-order duration-seconds gathi]
  (try
    (rs)
    (let [collated-swarams (get-or-add-collation raga-name swarams nth-order)]
      (rstart raga-name nth-order gathi)
      (playback/generic-play
       (take (* gathi duration-seconds)
             (mc/generate (repeat nth-order :.s) collated-swarams))
       (Math/pow gathi -1)))
    (catch Exception e
      (rs))))

(comment
  (def mohanam-swarams (doall (mapcat swarams samples/mohanam-files)))
  (def kalyani-swarams (doall (swarams-for-raga samples/kalyani-files 0.38)))
  (def collated-kalyani-swarams
    (doall
     (pmap (fn [nth-order]
             {nth-order (mc/collate kalyani-swarams nth-order)})
           (range 2 21))))

  (movertone.tanpura/play 60 0.2)
  (play-collated :kalyani kalyani-swarams 2 20 30)
  (play-collated :kalyani kalyani-swarams 10 30 100))
