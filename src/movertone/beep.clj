(ns movertone.beep
  (:use [overtone.core])
  (:require [movertone.gamakams :as g]))

(defmacro g-inst [g-name g-env]
  `(definst ~g-name [~'f 260 ~'lf 260 ~'pf 260 ~'d 1 ~'d1 0.1 ~'d4 0.4 ~'d5 0.5 ~'d9 0.9]
     (let [gamakam-env# ~g-env]
       (~'* (sin-osc (env-gen gamakam-env#))
            (env-gen (envelope [1 1 0] [~'d9 ~'d1]))))))

(g-inst sphuritam-inst
        (envelope [f  f  lf  f  f]
                  [ d4 d1  d1 d4]
                  :welch))

(g-inst plain-inst
        (envelope [f  f]
                  [d]
                  :welch))

(g-inst kampitam-inst
        (envelope [pf f pf f]
                  [d5 d1 d4]
                  :linear))

(def gamakams-dict
  {:plain plain-inst
   :sphuritam sphuritam-inst
   :kampitam kampitam-inst})

(defn with-synth-args [midi seconds gamakam]
  (let [f (midi->hz midi)
        pf (midi->hz (dec midi))
        lf (midi->hz (- midi 2))
        d seconds
        d1 (* 0.1 d)
        d4 (* 0.4 d)
        d5 (* 0.5 d)
        d9 (* 0.9 d)
        inst (get gamakams-dict gamakam)]
    (inst :f f :pf pf :lf lf :d d :d1 d1 :d4 d4 :d5 d5 :d9 d9)))
