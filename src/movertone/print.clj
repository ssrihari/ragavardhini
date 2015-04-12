(ns movertone.print
  (:use [hiccup.core]
        [hiccup.page])
  (:require [movertone.defs :as d]
            [clojure.string :as s]))

(defn ->printable [swarams & {:keys [bold?]}]
  (s/join ", " (map name swarams)))

(defn html-row [cell-type i ragam]
  (let [[ragam-name {:keys [arohanam avarohanam]}] (first ragam)]
    [:tr
     [cell-type i]
     [cell-type (name ragam-name)]
     [cell-type (->printable arohanam)]
     [cell-type (->printable avarohanam)]]))

(defn melakartha+janya-rows [[{:keys [num name]} janya-ragams]]
  (let [janya-ragams (sort-by #(first (keys %)) janya-ragams)]
    (cons
     (html-row :th num {name nil})
     (map (partial html-row :td) (range) janya-ragams))))

(defn html-rows []
  (mapcat melakartha+janya-rows
          (sort-by #(-> % first :num) d/janyas-of-melakarthas)))

(defn write-html [filename]
  (spit filename (html5
                  [:style "th {text-align: left;}"]
                  [:table
                   [:thead
                    [:tr [:th "No."] [:th "Name"] [:th "Arohanam"] [:th "Avarohanam"]]]
                   [:tbody
                    (html-rows)]])))

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
  (write-html "ragas/ragas-with-melas.html")
  (write-markdown "ragas/ragas.md" (concat d/janyas d/melakarthas)))
