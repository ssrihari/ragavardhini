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
(def tempo 120)
(def jathi 4)

(defn default-durations [num-swarams]
  (take num-swarams (repeatedly (constantly jathi))))

(defn simple-swarams->actual-swarams [ragam swarams]
  (let [simples (d/simples->ragam ragam)]
    (map simples swarams)))

(defn phrase
  ([ragam swarams durations speed]
     (phrase (simple-phrase->actual-phrase ragam swarams) durations speed))
  ([swarams durations speed]
     (let [durations (or durations
                         (default-durations (count swarams)))]
       (melody/phrase (map #(/ % (* speed jathi)) durations)
                      swarams))))

(defn swaram->midi [swaram]
  (let [shadjam (shruthi d/shruthis)
        swara-sthanam (d/swarams->notes swaram)]
    (+ shadjam swara-sthanam)))

(defmethod llive/play-note :default [{midi :pitch seconds :duration}]
  (let [freq (olive/midi->hz midi)]
    (beep/beep freq seconds)))

(defn play-phrase [phrase]
  (->> phrase
       (melody/where :time (melody/bpm tempo))
       (melody/where :duration (melody/bpm tempo))
       (melody/where :pitch swaram->midi)
       llive/play))

(defn play-arohanam-and-avarohanam [{:keys [arohanam avarohanam] :as ragam}]
  (play-phrase (phrase (concat arohanam avarohanam) nil)))

(play-phrase
 (concat
  (phrase (:vasanta d/janyas)
          [:s :g :m :d :n :s. :n :d :n :s.]
          [ 2  4  2  4  2  6   2  2  2  2]
          2)))
