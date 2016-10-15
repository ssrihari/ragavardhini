(ns movertone.scripts.charts
  (:use [incanter core stats charts io])
  (:require [overtone.core :as o]
            [movertone.swarams :as sw]
            [movertone.scripts.frequencies :as f]
            [movertone.scripts.samples :as samples]))

(defn melograph [filename]
  (let [freqs (f/freqs-from-file filename)]
    (save (xy-plot (range (count freqs))
                   freqs
                   :title (str "melograph for " filename)
                   :x-label "time (ms)")
          (str filename "-melograph.png")
          :width 1000
          :height 500)))

(defn pitch-histogram [filename]
  (let [freqs (f/freqs-from-file filename)
        midis (map o/hz->midi freqs)
        normalized-midis (map #(mod % 12) midis)
        swarams (f/freqs->swarams freqs)
        times (range (count midis))
        data-set (to-dataset (map (fn [f m n s t] {:freq f :midi m :nmidi n :swaram s :time t})
                                  freqs midis normalized-midis swarams times))]
    (with-data data-set
      (save (histogram :nmidi
                       :density true
                       :nbins 100
                       :title (str "pitch histogram for " filename)
                       :x-label "frequency"
                       :y-label "ocurrances")
            (str filename "-pitch-histogram.png")
            :width 1000
            :height 300))))

(defn swaram-histogram [osp filename]
  (let [data-set (to-dataset (map (fn [[s o]] {:swaram (name s) :occurance o})
                                  (sort-by #(sw/swarams->notes (first %)) < osp)))]
    (with-data data-set
      (save (bar-chart :swaram
                       :occurance
                       :title (str "swaram histogram for " filename)
                       :x-label "swaram"
                       :y-label "ocurrances")
            (str filename "-swaram-histogram.png")
            :width 1200
            :height 300))))
