(ns movertone.core
  (:require [overtone.live :as olive]
            [overtone.inst.piano :as piano]
            [movertone.violin :as violin]
            [movertone.beep :as beep]
            [leipzig.live :as llive]
            [leipzig.scale :as scale]
            [leipzig.melody :as melody]
            [movertone.defs :as d]))

(def shruthi :c)
(def tempo 90)
(def jathi 4)

(defn default-durations [num-swarams]
  (take num-swarams (repeatedly (constantly jathi))))

(defn phrase [swarams durations]
  (let [durations (or durations
                      (default-durations (count swarams)))]
    (melody/phrase (map #(/ % jathi) durations)
                   swarams)))

(defmethod llive/play-note :default [{midi :pitch seconds :duration}]
  (let [freq (olive/midi->hz midi)]
    (beep/beep freq seconds)))

(defn play-phrase [phrase]
  (->> phrase
       (melody/where :time (melody/bpm tempo))
       (melody/where :duration (melody/bpm tempo))
       (melody/where :pitch play-swaram)
       llive/play))

(defn play-arohanam-and-avarohanam [{:keys [arohanam avarohanam] :as ragam}]
  (play-phrase (phrase (concat arohanam avarohanam) nil)))
