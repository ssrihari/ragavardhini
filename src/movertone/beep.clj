(ns movertone.beep
  (:use [overtone.core])
  (:require [movertone.gamakams :as g]))

(defn my-sine [t]
  (* 0.5 (+ 1.0 (Math/sin (* Math/PI  (- t 0.5))))))

(defn string-pos-interp [t skew-point p1 p2]
  (if (= p1 p2)
    p1
    (let [l1 (Math/pow 2 (- (/ p1 12)))
          l2 (Math/pow 2 (- (/ p2 12)))
          frac (my-sine t)
          p (/ (* -12 (Math/log (+ l1 (* frac (- l2 l1))))) (Math/log 2))]
      p)))


(defn fret-slide [t skew-point p1 p2]
  (if (= p1 p2)
    0
    (let [p (string-pos-interp t skew-point p1 p2)
          p (min (Math/max p1 p2) (Math/ceil (- p 0.1)))]
      (/ (- p p1) (- p2 p1)))))

(definst beep [freq 440 dur 1.0]
  (def *f freq)
  (def *d dur)
  (-> freq
      saw
      (* (env-gen (perc 0.05 dur) :action FREE))))

(defn my-env [midi dur]
  (let [s (midi->hz 60)
        r (midi->hz 61)
        g (midi->hz 64)
        m (midi->hz 65)]
    (envelope [s s   r   r    s    r   r   g   g   m  m]
              [ 1 0.1 0.4 0.05 0.05 0.4 0.3 0.8 0.01 0.99]
              :welch)))

(defn jaru [n1 n2]
  (let [f1 (midi->hz n1)
        f2 (midi->hz n2)]
    (demo 2 (pan2 (sin-osc (env-gen (envelope [f1 f1 f2]
                                              [1 0.2]
                                              :welch)))))))

(defn kampitam [n1 n2]
  (let [f1 (midi->hz n1)
        f2 (midi->hz n2)]
    (demo 2 (pan2 (sin-osc (env-gen (envelope [f1 f1 f2]
                                              [1 0.2]
                                              :welch)))))))

(definst jinst [f1 261 f2 277]
  (pan2 (sin-osc (env-gen (envelope [f1 f1 f2]
                                    [1 0.2]
                                    :welch)))))

(def foo (env-gen (envelope [1 1 0] [0.9 0.1])))

(definst sphuritam-sine [f 262 lf 260 dur 1]
  (* (sin-osc (env-gen (envelope [f  f     lf    f   f]
                                 [(* 0.4 dur) (* 0.1 dur)  (* 0.1 dur) (* 0.4 dur)]
                                 :welch)))
     (env-gen (envelope [1 1 0] [(* dur 0.9) (* dur 0.1)]))))

(comment

  this works
  (* (sin-osc (env-gen (envelope [f  f     lf    f   f]
                                 [(* 0.4 dur) (* 0.1 dur)  (* 0.1 dur) (* 0.4 dur)]
                                 :welch)))
     (env-gen (envelope [1 1 0] [(* dur 0.9) (* dur 0.1)])))

  and this
  (let [m->h #(* 440.0 (java.lang.Math/pow 2.0 (/ (- % 69.0) 12.0)))
        f (m->h ~midi)
        lf (m->h (- ~midi 2))]
    (env-gen (envelope [f  f     lf    f   f]
                       [(* 0.4 dur) (* 0.1 dur)  (* 0.1 dur) (* 0.4 ~dur)]
                       :welch))))
