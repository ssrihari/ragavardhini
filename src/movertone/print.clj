(ns movertone.print
  (:use [hiccup.core]
        [hiccup.page])
  (:require [movertone.defs :as d]
            [clojure.string :as s]))

(defn ->printable [swarams]
  (s/join ", " (map name swarams)))

(defn html-rows [ragams]
  (map-indexed (fn [i [rname {:keys [arohanam avarohanam]}]]
                 [:tr
                  [:td i]
                  [:td (name rname)]
                  [:td (->printable arohanam)]
                  [:td (->printable avarohanam)]])
               ragams))

(defn write-html [filename ragas]
  (let [raga-rows (html-rows ragas)]
    (spit filename (html5
                    [:style "th {text-align: left;}"]
                    [:table
                     [:thead
                      [:tr [:th "No."] [:th "Name"] [:th "Arohanam"] [:th "Avarohanam"]]]
                     [:tbody
                      raga-rows]]))))

(defn markdown-rows [ragams]
  (map-indexed (fn [i [rname {:keys [arohanam avarohanam]}]]
                 (s/join "" (interpose "|" [i
                                            (name rname)
                                            (->printable arohanam)
                                            (->printable avarohanam)])))
               ragams))

(defn write-markdown [filename ragas]
  (let [raga-rows (markdown-rows ragas)
        header "|No.|Name|Arohanam|Avarohanam"
        line "|---|---|-----|-------"]
    (spit filename (s/join "\n" (concat [header line] raga-rows)))))

(comment
  (write-html "ragas.html" (concat d/janyas d/melakarthas))
  (write-markdown "ragas.md" (concat d/janyas d/melakarthas)))
