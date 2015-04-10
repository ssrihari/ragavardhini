(ns movertone.beep
  (:require [overtone.live :as olive]))

(olive/definst beep [freq 440 dur 1.0]
  (-> freq
      olive/saw
      (* (olive/env-gen (olive/perc 0.05 dur) :action olive/FREE))))
