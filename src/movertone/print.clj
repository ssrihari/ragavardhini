(ns movertone.print
  (:use [hiccup.core]
        [hiccup.page])
  (:require [movertone.defs :as d]))

(defn ->printable [swarams]
  (clojure.string/join ", " (map name swarams)))

(defn rows [ragams]
  (map-indexed (fn [i [rname {:keys [arohanam avarohanam]}]]
                 [:tr
                  [:td i]
                  [:td (name rname)]
                  [:td (->printable arohanam)]
                  [:td (->printable avarohanam)]])
               ragams))

(defn write-html [filename ragas]
  (let [raga-rows (rows ragas)]
    (spit filename (html5
                    [:style "th {text-align: left;}"]
                    [:table
                     [:thead
                      [:tr [:th "No."] [:th "Name"] [:th "Arohanam"] [:th "Avarohanam"]]]
                     [:tbody
                      raga-rows]]))))

(comment
  (write-html "ragas.html" (concat d/janyas d/melakarthas)))
