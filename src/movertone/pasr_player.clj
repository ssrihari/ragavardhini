(ns movertone.pasr-player.clj
  (:use [overtone.core])
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [overtone.live :as olive]
            [overtone.inst.piano :as piano]
            [movertone.violin :as violin]
            [leipzig.live :as llive]
            [leipzig.scale :as scale]
            [leipzig.melody :as melody]
            [movertone.pasr :as pasr]
            [movertone.beep :as beep]
            [movertone.swarams :as sw]
            [movertone.ragams :as r]
            [movertone.gamakams :as g]))

(def shruthi :c)
(def tempo 40)
(def jathi 4)
(def kalams {:lower  1
             :middle 2
             :higher 3})

(defn default-durations [num-swarams]
  (take num-swarams (repeatedly (constantly jathi))))

(defn ->jathi [speed jathi duration]
  (/ duration (* speed jathi)))

(defn phrase [pasr-phrase speed]
  (let [pasr-notes (pasr/make-pasr pasr-phrase)
        durations (map #(get-in % [:data :dur]) pasr-notes)
        ph-durations (map #(/ % (* speed jathi)) durations)]
    (melody/phrase ph-durations pasr-notes)))

(defmethod llive/play-note :default [{{{:keys [pre pasr nex]} :data} :pitch seconds :duration}]
  (let [[p a s r] pasr
        cur-midi (sw/rel-num->midi shruthi p)
        pre-midi (sw/rel-num->midi shruthi (or pre p))
        nex-midi (sw/rel-num->midi shruthi (or nex p))]
    (beep/with-synth-args pre-midi cur-midi nex-midi seconds a s r :pasr)))

(defn play-phrase [phrase]
  (->> phrase
       (melody/where :time (melody/bpm tempo))
       (melody/where :duration (melody/bpm tempo))
       llive/play))
