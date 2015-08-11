(ns movertone.beep
  (:use [overtone.core])
  (:require [movertone.gamakams :as g]))

(defmacro g-inst [g-name g-env]
  `(definst ~g-name [~'f 260
                     ~'lf 260
                     ~'pf 260

                     ~'d 1
                     ~'d1 0.1
                     ~'d2 0.2
                     ~'d3 0.3
                     ~'d4 0.4
                     ~'d5 0.5
                     ~'d9 0.9]
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
                  :welch))

(g-inst kampitam-inst-2
        (envelope [pf f pf f pf f f]
                  [d1 d2 d3 d1 d1 d2]
                  :welch))

(def gamakams-dict
  {:plain plain-inst
   :sphuritam sphuritam-inst
   :kampitam kampitam-inst
   :kampitam-2 kampitam-inst-2})

(defn with-synth-args [pre cur nex seconds gamakam]
  (let [pf (midi->hz pre)
        f (midi->hz cur)
        nf (midi->hz nex)
        lf (midi->hz (- cur 2))
        d seconds
        d1 (* 0.1 d)
        d2 (* 0.2 d)
        d3 (* 0.3 d)
        d4 (* 0.4 d)
        d5 (* 0.5 d)
        d9 (* 0.9 d)
        inst (get gamakams-dict gamakam)]
    (inst :f f :pf pf :lf lf
          :d d :d1 d1 :d2 d2 :d3 d3 :d4 d4 :d5 d5 :d9 d9)))
