(ns movertone.print
  (:use [hiccup.core]
        [hiccup.page])
  (:require [movertone.swarams :as d]
            [movertone.ragams :as r]
            [clojure.string :as s]))

(defn ->printable [swarams & {:keys [bold?]}]
  (s/join ", " (map (comp s/capitalize name) swarams)))

(defn row [mela? ragam]
  (let [[ragam-name {:keys [arohanam avarohanam]}] (first ragam)]
    [mela?
     (s/capitalize (name ragam-name))
     (->printable arohanam)
     (->printable avarohanam)]))

(defn melakartha+janya-rows [[{:keys [num name] :as mela-ragam} janya-ragams]]
  (let [janya-ragams (sort-by #(first %) janya-ragams)]
    (cons
     (row true {name mela-ragam})
     (map #(row false (into {} [%])) janya-ragams))))

(defn rows []
  (->> r/janyams-by-melakarthas
       (sort-by #(-> % first :num))
       (mapcat melakartha+janya-rows)))

(defn melakartha-rows []
  (->> r/melakarthas
       (sort-by #(-> % second :num))
       (map #(row false [%]))))

(defn html-rows
  ([] (html-rows (rows)))
  ([rows]
     (map-indexed (fn [i [mela? :as row]]
                    (let [class (if mela? "mela" "janya")
                          row-i (assoc row 0 i)
                          html-row (map (fn [c] [:td c]) row-i)]
                      [:tr {:class class} html-row]))
                  rows)))

(def css
  "table {
  color: #333;
  font-family: Helvetica, Arial, sans-serif;
  font-size: 12px;
  border-collapse: collapse; border-spacing: 0;
  }

  td, th { text-align:left;
  border: 1px solid #CCC;
  height: 22px;
  padding: 0 20px 0;
  } /* Make cells a bit taller */

  th {
  font-weight: bold;
  }

  .mela {
  font-weight: bold;
  background: #F1F1F1;
  }
")

(defn make-html [rows]
  (html5
   [:style css]
   [:table
    [:thead
     [:tr [:th "No."] [:th "Name"] [:th "Arohanam"] [:th "Avarohanam"]]]
    [:tbody
     rows]]))

(defn write-html [filename]
  (spit filename (make-html (html-rows))))

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
  (write-html "ragas/ragas.html")
  (write-markdown "ragas/ragas.md" (concat d/janyas d/melakarthas)))
