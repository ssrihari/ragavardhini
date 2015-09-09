(ns movertone.core
  (:use [overtone.core])
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
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

(def shruthi :e)
(def tempo 40)
(def jathi 4)
(def kalams {:lower  1
             :middle 2
             :higher 3})

(defn default-durations [num-swarams]
  (take num-swarams (repeatedly (constantly jathi))))

(defn simple-swaram->actual-swaram [ragam swaram]
  (get (sw/get-simple-swaram-mappings ragam)
       swaram))

(defrecord Triplet [swarams])

(defn make-triplets [swarams]
  (let [sw-list (flatten [nil swarams nil])]
    (->> sw-list
         (map-indexed (fn [i e] (Triplet. {:pre (-> (drop i sw-list) vec (get 0))
                                           :cur (-> (drop i sw-list) vec (get 1))
                                           :nex (-> (drop i sw-list) vec (get 2))})))
         (drop-last 2))))

(defn ->jathi [speed jathi duration]
  (/ duration (* speed jathi)))

(defn phrase
  ([ragam swarams durations speed]
     (phrase (map #(simple-swaram->actual-swaram ragam %) swarams) durations speed))
  ([swarams durations speed]
     (let [triplets (make-triplets swarams)
           durations (or durations
                         (default-durations (count swarams)))]
       (melody/phrase (map #(/ % (* speed jathi)) durations)
                      triplets))))

(defmethod llive/play-note :default [{{{:keys [pre cur nex]} :swarams} :pitch seconds :duration}]
  (let [get-midi #(some->> % :swaram )
        cur-midi (sw/swaram->midi shruthi (:swaram cur))
        pre-midi (sw/swaram->midi shruthi (or (:swaram pre) (:swaram cur)))
        nex-midi (sw/swaram->midi shruthi (or (:swaram nex) (:swaram cur)))]
    (beep/with-synth-args pre-midi cur-midi nex-midi seconds 0 0 0 (:gamakam cur))))

(defn play-phrase [phrase]
  (->> phrase
       (melody/where :time (melody/bpm tempo))
       (melody/where :duration (melody/bpm tempo))
       llive/play))

(defn play-arohanam-and-avarohanam [{:keys [arohanam avarohanam] :as ragam}]
  (play-phrase (phrase (concat arohanam avarohanam) nil (:lower kalams))))

(def gamakam-strs
  {"-" :plain
   "~" :kampitam-2
   "^" :sphuritam})

(defn swaram-str->swaram [ragam sw-str]
  {:gamakam (gamakam-strs (re-find #"[~^-]*" sw-str))
   :swaram (simple-swaram->actual-swaram ragam (keyword (re-find #"[.]*[srgmpdn][.]*" sw-str)))
   :duration (inc (get (frequencies sw-str) \, 0))})

(defn string->phrase [ragam phrase-str speed]
  (let [swarams (map #(swaram-str->swaram ragam %) (s/split phrase-str #" "))
        triplets (make-triplets swarams)
        durations (or (map :duration swarams)
                      (default-durations (count swarams)))]
    (melody/phrase (map #(->jathi speed jathi %) durations)
                   triplets)))

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

  (play-phrase (string->phrase (:mayamalavagaula r/ragams)
                               "-s, ~r, ^g, -m, ^p, ~d, ^n, -s.,"
                               1))

  (play-phrase
   (string->phrase (:mechakalyani r/ragams)
                   "-m -d ^n, -g -m ^d, -r -g -m, -g -m -d -n -s.,,,"
                   3))

  (play-string (:bilahari r/ragams)
               "s,,r g,p, d,s., n,d, p,dp mgrs rs .n .d s,,,
                s,,r g,p, m,,g p,d, r.,,s. n,d, p,,m g,r,")

  (play-file (:mohana r/ragams)
             "input-files/mohana-varnam.txt"))
