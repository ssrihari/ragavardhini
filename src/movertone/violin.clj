(ns movertone.violin
  (:use [overtone.live]))

(defsynth violin
  "violin inspired by Sound On Sound April-July 2003 articles."
  [pitch   {:default 60  :min 0   :max 127 :step 1}
   amp     {:default 1.0 :min 0.0 :max 1.0 :step 0.01}
   gate    {:default 1   :min 0   :max 1   :step 1}
   out-bus {:default 0   :min 0   :max 127 :step 1}]
  (let [freq   (midicps pitch)
        ;; 3b) portamento to change frequency slowly
        freqp  (slew:kr freq 100.0 100.0)
        ;; 3a) vibrato to make it seem "real"
        freqv  (vibrato :freq freqp :rate 6 :depth 0.02 :delay 1)
        ;; 1) the main osc for the violin
        saw    (saw freqv)
        ;; 2) add an envelope for "bowing"
        saw0   (* saw (env-gen (adsr 1.5 1.5 0.8 1.5) :gate gate :action FREE))
        ;; a low-pass filter prior to our filter bank
        saw1   (lpf saw0 4000) ;; freq???
        ;; 4) the "formant" filters
        band1  (bpf saw1 300 (/ 3.5))
        band2  (bpf saw1 700 (/ 3.5))
        band3  (bpf saw1 3000 (/ 2))
        saw2   (+ band1 band2 band3)
        ;; a high-pass filter on the way out
        saw3   (hpf saw2 30) ;; freq???
        ]
    (out out-bus (pan2 (* amp saw3)))))

;; just playing around...
(comment
  (def v0 (violin :pitch 60))
  (ctl v0 :pitch 64)
  (ctl v0 :pitch 67)
  (ctl v0 :pitch 72)
  (ctl v0 :pitch 60)
  (ctl v0 :gate 0)
)
