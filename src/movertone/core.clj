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
(def tempo 80)
(def jathi 4)

(defn default-durations [num-swarams]
  (take num-swarams (repeatedly (constantly jathi))))

(defn simple-phrase->actual-phrase [ragam swarams]
  (let [simples (d/get-simple-swaram-mappings ragam)]
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

(defn string->phrase [ragam s]
  (let [swaram "[.]*[A-z][.]*"
        swaram-with-duration (str swaram "[,]*")
        split-seq (re-seq (re-pattern swaram-with-duration) s)
        swarams (map #(keyword (re-find (re-pattern swaram) %)) split-seq)
        durations (map #(count (str/replace % #"\." "")) split-seq)]
    (phrase ragam swarams durations 1)))

(defn play-string [raga string]
  (play-phrase
   (string->phrase raga string)))

(comment
  (play-string (:bilahari d/janyas)
               "s,,r g,p, d,s., n,d, p,dp mgrs rs .n .d s,,,
                s,,r g,p, m,,g p,d, r.,,s. n,d, p,,m g,r,")

  (play-phrase
   (phrase (:mechakalyani d/melakarthas)
           [:m :d :n :g :m :d :r :g :m  :g :m :d :n :s.]
           [ 1  1  2  1  1  2  1  1  4   1  1  1  1  4]
           2)))
