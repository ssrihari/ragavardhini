(ns movertone.print
  (:use [hiccup.core]
        [hiccup.page])
  (:require [clojure.pprint :as pprint]
            [movertone.swarams :as d]
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

(defn make-html [rows]
  (html5
   (include-css "/style.css")
   [:table
    [:thead
     [:tr [:th "No."] [:th "Name"] [:th "Arohanam"] [:th "Avarohanam"]]]
    [:tbody
     rows]]))

(defn write-html [filename]
  (spit filename (make-html (html-rows))))

(defn display-ragam-name [ragam-name]
  (s/capitalize (name ragam-name)))

(defn ragam-url [ragam]
  (str "/search/" (name (:name ragam))))

(defn pretty-ragam-html
  [{:keys [avarohanam name arohanam num parent-mela-num parent-mela-name] :as ragam}]
  [:a {:href (ragam-url ragam)}
   [:div {:class "ragam"}
    [:h2 {:class "ragam-name"} (s/capitalize (display-ragam-name name))]
    [:p {:class "notes"} (->printable arohanam)]
    [:p {:class "notes"} (->printable avarohanam)]
    (if parent-mela-num
      [:p {:class "more-info"} (str "Janyam of " (display-ragam-name parent-mela-name) " (" parent-mela-num ")")]
      [:p {:class "more-info"} (str "This is Melakartha no. " num)])]])

(defn pretty-kriti [kriti]
  [:div
   [:p.kriti-name (:kriti kriti)]
   [:p.composer (:composer kriti)]])

(defn search-result-html [{:keys [ragam kritis more perc] :as search-result}]
  (html5
   (include-css "/style.css")
   [:meta {:name "viewport" :content "width=device-width"}]
   [:div {:class "search-result"}
    [:h1 "Best match"]
    (pretty-ragam-html ragam)
    (when (seq kritis)
      [:h1 "Kritis"])
    (when (seq kritis)
      [:ul.kritis (for [k (sort-by :kriti kritis)]
                    [:li.kriti (pretty-kriti k)])])
    (when (seq more)
      [:h1 "More..."])
    (when (seq more)
      [:ul {:class "more-results"}
       (map pretty-ragam-html more)])]))

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
