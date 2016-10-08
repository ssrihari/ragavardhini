(ns movertone.scripts.demo
  (:require [overtone.core :as o :refer [stop]]
            [movertone.scripts.frequencies :as f]
            [movertone.random :as r]
            [movertone.scripts.samples :as samples]))

(defn mohanam []
  (time
   (def mohanam-probs
     {:two (f/two-swaram-probabilities samples/mohanam-files)
      :one (f/one-swaram-probabilities samples/mohanam-files)})))

(comment
  (mohanam)
  (r/play-completely-random-phrase :mohana 4 100)
  (r/play-single-swaram-prob-phrase (:one mohanam-probs) 4 100)
  (r/play-with-two-swaram-weights (:two mohanam-probs) :.s 1/20 300))

(defn pancharatna-kritis []
  (time
   (def jagadanandakaraka-probs
     {:two (f/two-swaram-probabilities [(nth samples/pancharatna-kritis 0)])
      :one (f/one-swaram-probabilities [(nth samples/pancharatna-kritis 0)])}))

  (time
   (def dudukugala-probs
     {:two (f/two-swaram-probabilities [(nth samples/pancharatna-kritis 1)])
      :one (f/one-swaram-probabilities [(nth samples/pancharatna-kritis 1)])}))

  (time
   (def sadhinchane-probs
     {:two (f/two-swaram-probabilities [(nth samples/pancharatna-kritis 2)])
      :one (f/one-swaram-probabilities [(nth samples/pancharatna-kritis 2)])}))

  (time
   (def kanakanaruchira-probs
     {:two (f/two-swaram-probabilities [(nth samples/pancharatna-kritis 3)])
      :one (f/one-swaram-probabilities [(nth samples/pancharatna-kritis 3)])}))

  (time
   (def endaro-probs
     {:two (f/two-swaram-probabilities [(nth samples/pancharatna-kritis 4)])
      :one (f/one-swaram-probabilities [(nth samples/pancharatna-kritis 4)])})))
