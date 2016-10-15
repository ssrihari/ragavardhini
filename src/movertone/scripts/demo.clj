(ns movertone.scripts.demo
  (:require [overtone.core :as o :refer [stop]]
            [movertone.scripts.frequencies :as f]
            [movertone.random :as r]
            [movertone.scripts.samples :as samples]))

(defn mohanam []
  (time
   (def mohanam-probs
     (f/two-swaram-probabilities samples/mohanam-files
                                 :tonic-prominence 0.4 :npr-factor 0.25))))

(comment
  (mohanam)
  (r/play-completely-random-phrase :mohana 4 100)
  (r/play-single-swaram-prob-phrase (:one mohanam-probs) 4 100)
  (r/play-with-two-swaram-weights (:two mohanam-probs) :.s 1/20 300))

(defn pancharatna-kritis []
  (time
   (def jagadanandakaraka-probs
     (f/two-swaram-probabilities [(nth samples/pancharatna-kritis 0)]
                                 :tonic-prominence 0.4 :npr-factor 0.25)))

  (time
   (def dudukugala-probs
     (f/two-swaram-probabilities [(nth samples/pancharatna-kritis 1)]
                                 :tonic-prominence 0.4 :npr-factor 0.25)))

  (time
   (def sadhinchane-probs
     (f/two-swaram-probabilities [(nth samples/pancharatna-kritis 2)]
                                 :tonic-prominence 0.4 :npr-factor 0.25)))

  (time
   (def kanakanaruchira-probs
     (f/two-swaram-probabilities [(nth samples/pancharatna-kritis 3)]
                                 :tonic-prominence 0.4 :npr-factor 0.25)))

  (time
   (def endaro-probs
     (f/two-swaram-probabilities [(nth samples/pancharatna-kritis 4)]
                                 :tonic-prominence 0.4 :npr-factor 0.25))))
