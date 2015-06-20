(ns movertone.scripts.karnatic-scraper
  (:require [clojure.string :as s]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [medley.core :as m]
            [net.cgrand.enlive-html :as html]
            [clj-http.client :as client]
            [movertone.scripts.util :as util]
            [movertone.ragams :as r]
            [movertone.search :as search]))

(defn build-url [page]
  (str "http://www.karnatik.com/" page))

(defn foo [url]
  (let [response (client/get url)
        res (util/str->html-resource response)]
    res))

(defn song-name [elems]
  (let [song-name
        (->> (html/select elems [:b])
             (map html/text)
             (filter #(re-find #"Song:" %))
             first)]
    (-> song-name
        (s/replace #"Song:" "")
        (s/replace #"\\n" "")
        (s/replace #"\\r" "")
        (s/trim))))

(defn raga-name [elems]
  (let [raga-name
        (->> (html/select elems [:p :a])
             (map :attrs)
             (map :href)
             (filter #(re-find #"ragas" %))
             first)]
    (when raga-name
      (-> raga-name
          (s/replace #"ragas[A-z].shtml#" "")
          (s/replace #"\\" "")
          (s/replace #"\"" "")
          (s/trim)))))

(defn language [elems]
  (some->> (html/select elems [:p])
           (map html/text)
           (filter #(re-find #"Language:\W+" %))
           first
           (re-seq #"Language: \w+")
           first
           (#(s/split % #" "))
           second))

(defn taalam [elems]
  (some->> (html/select elems [:p])
           (map html/text)
           (filter #(re-find #"taaLam:\W+" %))
           first
           (re-seq #"taaLam: \w+")
           first
           (#(s/split % #" "))
           second))

(defn composer-name [elems]
  (->> (html/select elems [:p :a])
       (filter #(re-find #"co\d+.*" (get-in % [:attrs :href])))
       first
       :content
       first))

(defn lyrics [elems]
  (some->> (html/select elems [:td :p])
           (drop-last 4)
           (drop 3)
           (map html/text)
           (drop-while (complement #(re-find #"(?i)pallavi" %)))
           (map #(s/replace % #"\\r\\n\\r" ""))
           (map #(s/replace % #"\\n\\n" "\\\\n"))
           (map #(s/replace % #"^\\n|\\n$" ""))
           (map #(s/replace % #"\\n" "<br />"))
           (map s/trim)
           (map #(str "<p>" % "</p>"))))

(defn song-raga [url]
  (try
    (prn "fetching " url)
    (let [elems (foo url)]
      {:kriti    (song-name elems)
       :ragam    (raga-name elems)
       :composer (composer-name elems)
       :language (language elems)
       :taalam   (taalam elems)
       :lyrics   (lyrics elems)
       :url      url})
    (catch Exception e
      (prn "Caught exception when parsing " url)
      (.printStackTrace e)
      nil)))

(defn get-all-songs []
  (let [url "http://www.karnatik.com/lyrics.shtml"
        response (client/get url)
        res (util/str->html-resource response)
        options (html/select res [:option])]
    (->> options
         (map :attrs)
         (keep :value)
         (map #(s/replace % #"\\" ""))
         (map #(s/replace % #"\"" ""))
         (filter #(re-find #"c\d+.*" %)))))

(defn all-song-ragas []
  (let [all-pages (get-all-songs)
        urls (map build-url all-pages)]
    (pmap song-raga urls)))

(defn all-kritis []
  (let [data (doall (all-song-ragas))
        filename "all-karnatic-kritis.edn"]
    (spit (-> filename io/resource)
          (-> data pp/pprint with-out-str))))

(def kritis
  (r/read-file "all-karnatic-kritis.edn"))

(defn find-ragam-for-krithi [kriti]
  (let [search-result (:ragam (search/search-ragam (:ragam kriti)))]
    (merge kriti search-result)))

(defn pretty-name [kriti-name]
  (->> (s/split kriti-name #"_")
       (map s/capitalize)
       (s/join " ")))

(defn clean-name [kriti-name]
  (-> kriti-name
      (s/replace #"(?i)- click to.*" "")
      (s/replace #"\\n" "")
      (s/replace #"\\r" "")
      s/trim))

(defn prettify [{:keys [kriti composer url] :as k}]
  (when (and (seq kriti)
             (seq composer)
             (seq url))
    {:kriti (pretty-name (clean-name kriti))
     :composer (pretty-name (clean-name composer))
     :url url}))

(defn get-name->kritis []
  (let [name->kritis (group-by :name (map find-ragam-for-krithi kritis))
        n->ks (m/map-vals (fn [ks] (keep prettify ks))
                          name->kritis)]
    (r/write-file "raga-to-kritis-more.edn" n->ks)))
