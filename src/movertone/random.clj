(ns movertone.random
  (:use overtone.core)
  (:require [movertone.tanpura :as tanpura]
            [movertone.core-old :as c]
            [overtone.live :as olive]
            [medley.core :as m]
            [movertone.swarams :as sw]
            [movertone.ragams :as r]
            [movertone.scripts.frequencies :as f]))

(defn adjacent-differences [nums]
  (loop [num-list nums
         res []]
    (let [two-nums (take 2 num-list)]
      (if (= 2 (count two-nums))
        (recur (rest num-list)
               (conj res (Math/abs (apply - two-nums))))
        res))))

(defn gen-durations []
  (let [m 16
        n 8
        sum (- m n)]
    (->> (repeatedly n #(rand-nth (range (inc sum))))
         (cons sum)
         sort
         adjacent-differences
         (map inc))))

(defn random-swarams [swarams num]
  (repeatedly num #(rand-nth swarams)))

(defn random-durations [jathi num]
  ;; [1, 2, 1, 1, 4, 1, 1, 2, 2, 4]
  (take num (flatten (repeatedly gen-durations))))

(defn random-phrase [ragam-name jathi num]
  (let [ragam (ragam-name r/ragams)
        swarams-in-ragam (sw/actual-swarams->simple-swarams
                          (sw/madhya-swarams-in-ragam ragam))]
    (c/phrase ragam
              (vec (random-swarams swarams-in-ragam num))
              (vec (random-durations jathi num))
              (:lower c/kalams))))

(defn swaram-allocations [ragam-hist]
  (->>
   (reduce (fn [allocations [swaram perc]]
             (let [[_ [b e]] (last allocations)
                   beg (if e (+ 0.00000000000001 e) 0)]
               (conj allocations [swaram [beg (+ beg perc)]])))
           []
           (sort-by second > ragam-hist))
   (into {})))

(defn ts-swaram-allocations [ts-hist]
  (m/map-vals swaram-allocations ts-hist))

(defn get-next-swaram [swaram-allocations]
  (let [r (rand 100)]
    (->> swaram-allocations
         (filter (fn [[swaram [b e]]] (< b r e)))
         first
         first)))

(defn get-next-swaram-from-two-swaram-prob [fswaram ts-sw-allocations]
  (get-next-swaram (get ts-sw-allocations fswaram)))

(defn prob-swaram-generator [fswaram ts-sw-allocations]
  (lazy-seq
   (let [next-swaram (get-next-swaram-from-two-swaram-prob fswaram ts-sw-allocations)]
     (cons fswaram (prob-swaram-generator next-swaram ts-sw-allocations)))))

(defn get-next-swaram-from-three-swaram-prob [ft-swarams ts-sw-allocations]
  (get-next-swaram (get ts-sw-allocations ft-swarams)))

(defn prob-swaram-generator-for-three [[f s] ts-sw-allocations]
  (lazy-seq
   (let [next-swaram (get-next-swaram-from-three-swaram-prob [f s] ts-sw-allocations)]
     (cons next-swaram (prob-swaram-generator-for-three [s next-swaram] ts-sw-allocations)))))

(defn weighted-random-phrase
  ([ragam-hist jathi num]
   (weighted-random-phrase ragam-hist jathi num (:lower c/kalams)))
  ([ragam-hist jathi num gathi]
   (let [sw-allocations (swaram-allocations ragam-hist)
         r-swarams (vec (repeatedly num #(get-next-swaram sw-allocations)))]
     (prn r-swarams)
     (c/phrase r-swarams
               (vec (random-durations jathi num))
               gathi))))

(defn ts-weighted-random-phrase [ts-hist fswaram gathi num]
  (let [ts-allocations (ts-swaram-allocations ts-hist)
        r-swarams (vec (take num (prob-swaram-generator fswaram ts-allocations)))]
    (prn r-swarams)
    (c/phrase r-swarams
              (vec (take num (repeat 1)))
              gathi)))

(defn ts-weighted-random-phrase-nth [ts-hist fswaram gathi num]
  (let [ts-allocations (ts-swaram-allocations ts-hist)
        r-swarams (vec (take num (take-nth 40 (prob-swaram-generator fswaram ts-allocations))))]
    (prn r-swarams)
    (c/phrase r-swarams
              (vec (take num (repeat 1)))
              gathi)))

(definst ignore-this [x 1]
  (def *mx *)
  (sin-osc))

(defn ampl-env [phrase-duration]
  (let [dp5 (* 0.05 phrase-duration)
        d9 (* 0.9 phrase-duration)]
    (envelope [0 1 1 0] [dp5 d9 dp5])))

(defmacro play-in-carnatic-style [pitch-env ampl-env dur]
  `((overtone.sc.synth/synth
     "audition-synth"
     (out 0 (hold
             (*mx (sin-osc (env-gen ~pitch-env))
                  (env-gen ~ampl-env))
             ~dur
             :done FREE)))))

(defn generic-play [swarams gathi num]
  (let [freqs (->> swarams
                   (mapv (partial sw/swaram->midi :c.))
                   (mapv midi->hz))
        durations (vec (take num (repeat gathi)))
        total-duration (* num gathi)
        pitch-env (envelope freqs durations)]
    (play-in-carnatic-style pitch-env
                            (ampl-env total-duration)
                            total-duration)))

(defn play-with-two-swaram-weights
  ([ts-hist fswaram gathi num]
   (let [ts-allocations (ts-swaram-allocations ts-hist)
         pitches (vec (take num (prob-swaram-generator fswaram ts-allocations)))]
     (play-with-two-swaram-weights pitches gathi num)))
  ([pitches gathi num]
   (recording-start (str "resources/kosha/" (gensym "two-") ".wav"))
   (let [freqs (->> pitches
                    (mapv (partial sw/swaram->midi :c.))
                    (mapv midi->hz))
         _ (prn pitches)
         durations (vec (take num (repeat gathi)))
         total-duration (* num gathi)
         pitch-env (envelope freqs durations)]
     (tanpura/play 60 0.2)
     (play-in-carnatic-style pitch-env
                             (ampl-env total-duration)
                             total-duration))))

(defn play-with-three-swaram-weights
  ([ts-hist ft-swarams gathi num]
   (let [ts-allocations (ts-swaram-allocations ts-hist)
         pitches (vec (take num (prob-swaram-generator-for-three ft-swarams ts-allocations)))]
     (play-with-three-swaram-weights pitches gathi num)))
  ([pitches gathi num]
   (recording-start (str "resources/kosha/" (gensym "three-") ".wav"))
   (let [freqs (->> pitches
                    (mapv (partial sw/swaram->midi :c.))
                    (mapv midi->hz))
         _ (prn pitches)
         durations (vec (take num (repeat gathi)))
         pitch-env (envelope freqs durations)]
     (tanpura/play 60 0.2)
     (demo 60 (pan2
               (*mx (sin-osc (env-gen pitch-env))
                    (env-gen (ampl-env (* num gathi)))))))))

(defn play-random-phrase [ragam-name jathi num]
  (let [phrase (random-phrase ragam-name jathi num)]
    (prn (map :pitch phrase))
    (c/play-phrase phrase)))

(def play-completely-random-phrase play-random-phrase)
(def single-swaram-prob-phrase weighted-random-phrase)

(defn play-single-swaram-prob-phrase [single-swaram-prob jathi num]
  (recording-start (str "resources/kosha/" (gensym "one-") ".wav"))
  (c/play-phrase (single-swaram-prob-phrase single-swaram-prob jathi num)))

(comment
  (definst kick [freq 120 dur 0.3 width 0.5]
    (let [freq-env (* freq (env-gen (perc 0 (* 0.99 dur))))
          env (env-gen (perc 0.01 dur) 1 1 0 1 FREE)
          sqr (* (env-gen (perc 0 0.01)) (pulse (* 2 freq) width))
          src (sin-osc freq-env)
          drum (+ sqr (* env src))]
      (compander drum drum 0.2 1 0.1 0.01 0.01)))

  (definst c-hat [amp 0.8 t 0.04]
    (let [env (env-gen (perc 0.001 t) 1 1 0 1 FREE)
          noise (white-noise)
          sqr (* (env-gen (perc 0.01 0.04)) (pulse 880 0.2))
          filt (bpf (+ sqr noise) 9000 0.5)]
      (* amp env filt)))

  (def bpm 72)
  (def metro (metronome bpm))
  (defn player [beat]
    (at (metro beat) (kick))
    (at (metro (+ 0.5 beat)) (c-hat))
    (apply-by (metro (inc beat)) #'player (inc beat) []))
  )

(comment
  (let [nattai-allocations (swaram-allocations f/hist)]
    (get-next-swaram nattai-allocations)))

(comment
  (play-completely-random-phrase :nata 4 20)
  (c/play-phrase (single-swaram-prob-phrase f/sh 4 20))
  (c/play-phrase (ts-weighted-random-phrase f/tsh :.s 15 1000))

  (play-random-phrase :nata 3 100)
  (leipzig.live/stop)

  (foo f/x :.s 1/10 100)

  (def tsalls ))
