(ns movertone.scripts.demo
  (:require [overtone.core :as o]
            [leipzig.live :as l]
            [movertone.scripts.charts :as charts]
            [movertone.scripts.frequencies :as f]
            [movertone.random :as r]
            [movertone.scripts.samples :as samples]))

(defn stop []
  (l/stop)
  (o/stop)
  (o/recording-stop))

(defn mohanam []
  (time
   (def mohanam-probs
     (f/two-swaram-probabilities samples/mohanam-files
                                 :tonic-prominence 1 :npr-factor 1))))

(comment
  (mohanam)
  (r/play-completely-random-phrase :mohana 4 100)
  (r/play-single-swaram-prob-phrase (:one mohanam-probs) 4 20)
  (r/play-with-two-swaram-weights (:two mohanam-probs) :.s 1/20 300))

(defn revati []
  (time
   (def revati-probs
     (f/three-swaram-probabilities samples/revati-files
                                   :tonic-prominence 1 :npr-factor 0.5))))

(comment
  (revati)
  (r/play-completely-random-phrase :revati 4 100)
  (r/play-single-swaram-prob-phrase (:one revati-probs) 4 100)
  (r/play-with-two-swaram-weights (:two revati-probs) :.s 1/20 300)
  (r/play-with-three-swaram-weights (:three revati-probs) :.s 1/20 300))

(defn kalyani []
  (time
   (def kalyani-probs
     (f/three-swaram-probabilities samples/kalyani-files
                                   :tonic-prominence 1 :npr-factor 0.38))))

(comment
  (kalyani)
  (charts/swaram-histogram (:one kalyani-probs) "kalyani")
  (r/play-completely-random-phrase :kalyani 4 100)
  (r/play-single-swaram-prob-phrase (:one kalyani-probs) 4 100)
  (r/play-with-two-swaram-weights (:two kalyani-probs) :.s 1/20 300)
  (r/play-with-three-swaram-weights (:three kalyani-probs) :.s 1/20 300))

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
