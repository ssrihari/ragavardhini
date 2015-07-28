(ns movertone.gamakams
  (:use [overtone.core]))

(defmacro m->h [note]
  `(* 440.0 (java.lang.Math/pow 2.0 (/ (- ~note 69.0) 12.0))))

(defmacro sphuritam [midi dur]
  `(let [f# (* 440.0 (java.lang.Math/pow 2.0 (/ (- ~midi 69.0) 12.0)))
         lf# (* 440.0 (java.lang.Math/pow 2.0 (/ (- ~midi 2 69.0) 12.0)))]
     (env-gen (envelope [f#  f#     lf#    f#   f#]
                        [(* 0.4 ~dur) (* 0.1 ~dur)  (* 0.1 ~dur) (* 0.4 ~dur)]
                        :welch))))

(defn sph [midi dur]
  (let [f (midi->hz midi)
        lf (midi->hz (- midi 2))]
    (env-gen (envelope [f  f     lf    f   f]
                       [(* 0.4 dur) (* 0.1 dur)  (* 0.1 dur) (* 0.4 dur)]
                       :welch))))
