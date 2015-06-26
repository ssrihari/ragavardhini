(ns movertone.html
  (:use [hiccup.core]
        [hiccup.page])
  (:require [clojure.pprint :as pprint]
            [movertone.swarams :as d]
            [movertone.ragams :as r]
            [clojure.string :as s]))

(defn ->printable [swarams & {:keys [bold?]}]
  (s/join ", " (map (comp s/capitalize name) swarams)))

(defn display-ragam-name [ragam-name]
  (s/capitalize (name ragam-name)))

(defn ragam-url [ragam]
  (str "/search/ragam/" (name (:name ragam))))

(defn kriti-url [kriti]
  (str "/kritis/" (s/replace (:kriti kriti) #"[ \(\)]" "")))

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

(defn html-skeleton [body]
  (html5
   (include-css "/style.css")
   (include-css "http://fonts.googleapis.com/css?family=PT+Serif")
   (include-css "http://fonts.googleapis.com/css?family=PT+Sans")
   [:meta {:name "viewport" :content "width=device-width"}]
   [:form.search-form {:action "/search"}
    [:input.search-box {:type "text" :name "q" :placeholder "search..."}]
    [:input.submit-button {:name "t" :type "submit" :value "ragam"}]
    [:input.submit-button {:name "t" :type "submit" :value "kriti"}]]
   body))

(defn make-html [rows]
  (html-skeleton
   [:table
    [:thead
     [:tr [:th "No."] [:th "Name"] [:th "Arohanam"] [:th "Avarohanam"]]]
    [:tbody
     rows]]))

(defn pretty-ragam
  [{:keys [avarohanam name arohanam num parent-mela-num parent-mela-name] :as ragam}]
  [:a {:href (ragam-url ragam)}
   [:div {:class "ragam"}
    [:h2 {:class "ragam-name"} (display-ragam-name name)]
    [:p {:class "notes"} (->printable arohanam)]
    [:p {:class "notes"} (->printable avarohanam)]
    (if parent-mela-num
      [:p {:class "more-info"} (str "Janyam of " (display-ragam-name parent-mela-name) " (" parent-mela-num ")")]
      [:p {:class "more-info"} (str "This is Melakartha no. " num)])]])

(defn pretty-kriti [{:keys [url ragam kriti composer taalam language] :as kriti-result}]
  [:li.kriti
   (if (seq url)
     [:a {:href (kriti-url kriti-result)}
      [:p.kriti-name kriti]]
     [:p.kriti-name kriti])
   [:p.composer composer]
   (when ragam
     [:p.kriti-ragam
      [:a.kriti-ragam-link {:href (ragam-url {:name ragam})}
       (display-ragam-name ragam)]
      (when taalam (str ", " taalam))
      (when language (str ", " language))])])

(defn pretty-lyrics [lyric-lines]
  (let [heading? #(re-find #"(?i)pallavi|anupallavi|caranam" %)
        bolden #(str "<b class='lyrics-heading'>" % "</b>")
        lyricify #(str "<div class='lyrics-content'>" % "</div>")
        bolded (map #(if (heading? %) (bolden %) (lyricify %)) lyric-lines)]
    (s/join bolded)))

(defn pretty-thing [type thing]
  (case type
    :kriti (pretty-kriti thing)
    :ragam (pretty-ragam thing)))

(defn more-of [heading coll class-name]
  (when (seq coll)
    [:div.more-of
     [:h1.more-heading (str heading "..")]
     [:ul {:class class-name}
      coll]]))

(defn best-match [heading result type]
  (when result
    [:div.best-match
     [:h1 heading]
     (pretty-thing type result)]))

(defn search-ragam-result [{:keys [ragam kritis more-ragams perc] :as search-result}]
  (html-skeleton
   [:div {:class "search-result"}

    (best-match "Best ragam match"
                ragam
                :ragam)

    (more-of (str "Kritis in " (display-ragam-name (:name ragam)))
             (map pretty-kriti (sort-by :kriti kritis))
             "kritis-in-ragam")

    (more-of "More matching ragams"
             (map pretty-ragam more-ragams)
             "more-ragams")]))

(defn search-kriti-result [{:keys [kriti more-kritis] :as search-result}]
  (html-skeleton
   [:div {:class "search-result"}

    (best-match "Best kriti match"
                kriti
                :kriti)

    (more-of "More marching kritis"
             (map pretty-kriti more-kritis)
             "more-kritis")

    (when (seq (:lyrics kriti))
      [:h1 "Lyrics for \"" (:kriti kriti) "\""])
    (when (seq (:lyrics kriti))
      [:div.lyrics
       (pretty-lyrics (:lyrics kriti))])]))

(defn show-kriti [{:keys [kriti more-kritis] :as search-result}]
  (let [{kriti-name :kriti :keys [composer url taalam language]} kriti
        ragam (r/ragams (:ragam kriti))]
    (html-skeleton
     [:div.search-result
      [:h1.show-kriti (:kriti kriti)]

      [:div.kriti-blurb
       [:p.composer-line composer
        (when taalam (str ", " taalam))
        (when language (str ", " language))]

       (when ragam
         [:div.kriti-ragam
          [:a.kriti-ragam-link {:href (ragam-url ragam)}
           (display-ragam-name (:name ragam))]
          [:p.notes (->printable (:arohanam ragam))]
          [:p.notes (->printable (:avarohanam ragam))]
          (if (:parent-mela-num ragam)
            [:p.more-info
             (str "Janyam of "
                  (display-ragam-name (:parent-mela-name ragam))
                  " (" (:parent-mela-num ragam) ")")]
            [:p.more-info (str "This is Melakartha no. " (:num ragam))])])]
      [:br]
      (when (seq (:lyrics kriti))
        [:h2 "Lyrics"])
      (when (seq (:lyrics kriti))
        [:div.lyrics
         (pretty-lyrics (:lyrics kriti))])

      (when (seq url)
        [:a {:href url :target "_blank"} "See in karnatik"])

      (more-of "More marching kritis"
               (map pretty-kriti more-kritis)
               "more-kritis")])))
