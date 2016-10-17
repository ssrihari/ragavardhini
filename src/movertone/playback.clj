(ns movertone.playback
  (:use overtone.core)
  (:require [movertone.tanpura :as tanpura]
            [movertone.core-old :as c]
            [overtone.live :as olive]
            [medley.core :as m]
            [movertone.swarams :as sw]
            [movertone.ragams :as r]
            [movertone.scripts.frequencies :as f]
            [movertone.random :as random-play]))

(definst ignore-this [x 1]
  (def *mx *)
  (sin-osc))

(defn ampl-env [phrase-duration]
  (let [dp5 (* 0.01 phrase-duration)
        d9 (* 0.99 phrase-duration)]
    (envelope [0 1 1 0] [dp5 d9 dp5])))

(defmacro play-in-carnatic-style [pitch-env ampl-env dur]
  `((overtone.sc.synth/synth
     "audition-synth"
     (out 0 (hold
             (*mx (sin-osc (env-gen ~pitch-env))
                  (env-gen ~ampl-env))
             ~dur
             :done FREE)))))

(defn generic-play [swarams gathi]
  (let [num (count swarams)
        freqs (->> swarams
                   (mapv (partial sw/swaram->midi :c.))
                   (mapv midi->hz))
        durations (vec (repeat num gathi))
        total-duration (* num gathi)
        pitch-env (envelope freqs durations)]
    (prn swarams)
    (prn :total-duration total-duration)
    #_(recording-start (str "resources/kosha/" (gensym "generic-play-") ".wav"))
    (play-in-carnatic-style pitch-env
                            (ampl-env total-duration)
                            total-duration)))

(defn foo [file gathi]
  (let [swarams (f/freqs->swarams (f/freqs-from-file file))]
    (prn (count swarams))
    (def *s swarams)
    (generic-play (vec (take 1800 swarams)) gathi)))
