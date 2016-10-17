(ns movertone.scripts.charts
  (:use [incanter core stats charts io])
  (:require [overtone.core :as o]
            [clojure.string :as s]
            [movertone.swarams :as sw]
            [movertone.scripts.frequencies :as f]
            [movertone.scripts.samples :as samples]))

(defn melograph [filename]
  (let [freqs (f/freqs-from-file filename)
        [min-y max-y] ((juxt first last) (sort freqs))]
    (-> (xy-plot (range (count freqs))
                 freqs
                 :title (str "melograph for " filename)
                 :x-label "time (ms)")
        (set-y-range (- min-y 50) (+ max-y 50))
        (set-theme :default)
        (save (str filename "-melograph.png")
              :width 1200
              :height 300))))

(defn pitch-histogram [filename & [plot-axis]]
  (let [freqs (f/freqs-from-file filename)
        midis (map o/hz->midi freqs)
        normalized-midis (map #(mod % 12) midis)
        swarams (f/freqs->swarams freqs)
        times (range (count midis))
        data-set (to-dataset (map (fn [f m n s t] {:freq f :midi m :nmidi n :swaram s :time t})
                                  freqs midis normalized-midis swarams times))]
    (with-data data-set
      (save (histogram (or plot-axis :freq)
                       :density true
                       :nbins 1200
                       :title (str "pitch histogram for " filename)
                       :x-label "frequency"
                       :y-label "ocurrances")
            (str filename "-pitch-histogram.png")
            :width 1200
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
            :width 1300
            :height 300))))

(defn two-swaram-histogram [tsp filename]
  (let [data-set (->> tsp
                      (remove (fn [[[f s]]] (= f s)))
                      (sort-by second >)
                      (take 20)
                      (map (fn [[s o]] {:swarams (s/join ">" s) :occurance o}))
                      to-dataset)]
    (with-data data-set
      (save (bar-chart :swarams
                       :occurance
                       :title (str "two swaram histogram for " filename)
                       :x-label "swaram"
                       :y-label "ocurrances")
            (str filename "-two-swaram-histogram.png")
            :width 2000
            :height 300))))
