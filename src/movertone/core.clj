(ns movertone.core
  (:use [overtone.core])
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [overtone.live :as olive]
            [overtone.inst.piano :as piano]
            [movertone.violin :as violin]
            [leipzig.live :as llive]
            [leipzig.scale :as scale]
            [leipzig.melody :as melody]
            [movertone.beep :as beep]
            [movertone.swarams :as sw]
            [movertone.ragams :as r]
            [movertone.gamakams :as g]))

(def shruthi :c)
(def tempo 60)
(def jathi 4)
(def kalams {:lower  1
             :middle 2
             :higher 3})

(defn default-durations [num-swarams]
  (take num-swarams (repeatedly (constantly jathi))))

(defn simple-phrase->actual-phrase [ragam swarams]
  (let [simples (sw/get-simple-swaram-mappings ragam)]
    (map simples swarams)))

(defn phrase
  ([ragam swarams durations speed]
     (phrase (simple-phrase->actual-phrase ragam swarams) durations speed))
  ([swarams durations speed]
     (let [durations (or durations
                         (default-durations (count swarams)))]
       (melody/phrase (map #(/ % (* speed jathi)) durations)
                      swarams))))

(defmethod llive/play-note :default [{midi :pitch seconds :duration}]
  (beep/with-synth-args midi seconds :sphuritam))

(defn play-phrase [phrase]
  (->> phrase
       (melody/where :time (melody/bpm tempo))
       (melody/where :duration (melody/bpm tempo))
       (melody/where :pitch (partial sw/swaram->midi shruthi))
       llive/play))

(defn play-arohanam-and-avarohanam [{:keys [arohanam avarohanam] :as ragam}]
  (play-phrase (phrase (concat arohanam avarohanam) nil (:lower kalams))))

(defn string->phrase [ragam s]
  (let [swaram "[.]*[A-z][.]*"
        swaram-with-duration (str swaram "[,]*")
        split-seq (re-seq (re-pattern swaram-with-duration) s)
        swarams (map #(keyword (re-find (re-pattern swaram) %)) split-seq)
        durations (map #(count (s/replace % #"\." "")) split-seq)]
    (phrase ragam swarams durations (:lower kalams))))

(defn play-string [raga string]
  (play-phrase
   (string->phrase raga string)))

(defn play-file [raga filename]
  (play-string raga
               (-> filename io/resource slurp (s/replace #"\n" ""))))

(comment

  (:ragavardhini r/ragams)
  > {:arohanam [:s :r3 :g3 :m1 :p :d1 :n2 :s.],
     :avarohanam (:s. :n2 :d1 :p :m1 :g3 :r3 :s)}

  (play-arohanam-and-avarohanam (:hanumatodi r/ragams))

  (play-arohanam-and-avarohanam (:vasanta r/ragams))

  (play-phrase (phrase [:s :r2 :g3 :p :m1 :g3 :r2 :s]
                       [ 1   1  1  1   1   1   2   4]
                       (:lower kalams)))

  (play-phrase
   (phrase (:mechakalyani r/ragams)
           [:m :d :n :g :m :d :r :g :m  :g :m :d :n :s.]
           [ 1  1  2  1  1  2  1  1  4   1  1  1  1  4]
           (:middle kalams)))


  (play-string (:bilahari r/ragams)
               "s,,r g,p, d,s., n,d, p,dp mgrs rs .n .d s,,,
                s,,r g,p, m,,g p,d, r.,,s. n,d, p,,m g,r,")

  (play-file (:mohana r/ragams)
             "input-files/mohana-varnam.txt"))
