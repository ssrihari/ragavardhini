(ns movertone.pasr
  (:use overtone.core)
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.pprint :as pp]
            [cheshire.core :as json]))

(defn read-pasr-json [filename]
  (-> filename io/resource slurp (json/decode true)))

;; rendition < sections < phrases < plucks < pasrs

(def sahana-pasr
  (read-pasr-json "sriku-js/pasr-varnam.json"))

(def first-phrase
  (-> sahana-pasr first :pasr first))

(defn phrase->plucks [phrase]
  (->> phrase
       (map #(nth % 3))
       (apply concat)
       (map (fn [[p a s r]] (if (coll? p)
                              [(first p) a s r]
                              [p a s r])))))

(defn duration [pluck]
  (or (some-> pluck first (s/split #":") second Integer/parseInt)
      1))

(defrecord PASRUnit [data])

(defn make-pitches [pasr base]
  (->> pasr
       (mapcat (juxt first first))
       (map #(if (coll? %) (first %) %))
       (map #(midi->hz (+ base %)))))

(defn make-durations [pasr jathi]
  (let [durations-1 (->> pasr
                         (mapcat #(drop 1 %))
                         butlast
                         (drop 1))
        durations-2 (map-indexed (fn [i d]
                                   (cond (= 0 (mod i 3))
                                         d

                                         (= 1 (mod i 3))
                                         (+ d (nth durations-1 (inc i)))

                                         :else
                                         nil))
                                 durations-1)
        durations (remove nil? durations-2)]
    (map #(/ % jathi) durations)))

(defn make-env [shruthi jathi pluck]
  (let [pasr (nth pluck 3)
        pitches (make-pitches pasr shruthi)
        durations (make-durations pasr jathi)
        phrase-duration (duration pluck)]
    {:dur phrase-duration
     :env (envelope pitches
                    durations)}))

(defn envs-for-phrase [shruthi jathi phrase]
  (let [envs (map #(make-env shruthi jathi %)
                  phrase)
        ats (reductions + (map :dur envs))]
    (map #(assoc %1 :at %2) envs ats)))

(def one-twenty-bpm (metronome 120))

(defn make-pasr [phrase]
  (let [plucks (phrase->plucks phrase)]
    (map-indexed (fn [i [p a s r :as pasr]]
                   (PASRUnit. {:pasr pasr
                               :dur (+ a s r)
                               :pre (first (when (pos? i) (nth plucks (dec i))))
                               :nex (first (when (< i (dec (count plucks))) (nth plucks (inc i))))}))
                 plucks)))


(comment
  ;; potentially
  (demo 2 (pan2 (sin-osc (env-gen (envelope (map #(midi->hz (+ 60 %)) [4 4 2 2 7 7])
                                            (map #(/ % 8) [0 2 0 2 4]))))))
  )
