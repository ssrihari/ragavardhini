(ns movertone.core
  (:use [overtone.live]
        [overtone.inst.piano]
        [movertone.violin]
        [movertone.defs]))

(def nome (metronome 180))

(defn play-swaram [swaram]
  (let [n (swarams->notes swaram)]
    (piano :note (+ 60 n) :decay 0.01)))

(defn play-swarams [swarams]
  (when (seq swarams)
    (let [beat (nome)]
      (at (nome beat) (play-swaram (first swarams)))
      (apply-by (nome (inc beat)) play-swarams (rest swarams) []))))

(defn with-ragam [ragam swarams]
  (play-swarams (map ragam swarams)))

(defn play-ragam [ragam]
  (with-ragam (melakarthas ragam)
    arohanam+avarohanam))
