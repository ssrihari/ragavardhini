(ns movertone.beep
  (:use [overtone.core])
  (:require [movertone.gamakams :as g]))

(defmacro g-inst [g-name g-env]
  `(definst ~g-name [~'f 260
                     ~'lf 260
                     ~'pf 260
                     ~'nf 260
                     ~'a 1
                     ~'s 1
                     ~'r 1
                     ~'d 1
                     ~'dp5 0.05
                     ~'d1 0.1
                     ~'d2 0.2
                     ~'d3 0.3
                     ~'d4 0.4
                     ~'d5 0.5
                     ~'d9 0.9]
     (let [gamakam-env# ~g-env]
       (~'* (sin-osc (env-gen gamakam-env#))
            (env-gen (envelope [0 1 1 0] [~'dp5 ~'d9 ~'dp5]))))))

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

(g-inst pasr-inst
        (envelope [pf f f nf]
                  [ a s r]
                  :welch))

(def gamakams-dict
  {:plain      {:inst plain-inst
                :inputs [:d :f]}
   :sphuritam  {:inst sphuritam-inst
                :inputs [:f :lf :d1 :d4]}
   :kampitam   {:inst kampitam-inst
                :inputs [:f :pf :d1 :d4 :d5]}
   :kampitam-2 {:inst kampitam-inst-2
                :inputs [:f :pf :d1 :d2 :d3]}
   :pasr       {:inst pasr-inst
                :inputs [:f :pf :nf :a :s :r]}})

(defn with-synth-args [pre cur nex seconds a s r gamakam]
  (let [d seconds
        params {:pf (midi->hz pre)
                :f  (midi->hz cur)
                :nf (midi->hz nex)
                :lf (midi->hz (- cur 2))
                :a  a
                :s  s
                :r  r
                :d  seconds
                :dp5 (* 0.05 d)
                :d1 (* 0.1 d)
                :d2 (* 0.2 d)
                :d3 (* 0.3 d)
                :d4 (* 0.4 d)
                :d5 (* 0.5 d)
                :d9 (* 0.9 d)}
        {:keys [inst inputs]} (get gamakams-dict gamakam)
        args (flatten (vec (select-keys params (conj inputs :d9 :d1 :dp5))))]
    (apply inst args)))

(comment
  :f f :pf pf :a 1 :nf nf
    :d d :d1 d1 :d2 d2 :d3 d3 :d4 d4 :d5 d5 :d9 d9)
