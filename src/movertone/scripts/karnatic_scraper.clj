(ns movertone.scripts.karnatic-scraper
  (:require [clojure.string :as s]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [medley.core :as m]
            [net.cgrand.enlive-html :as html]
            [clj-http.client :as client]
            [movertone.scripts.util :as util]
            [movertone.ragams :as r]))

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

(defn song-raga [url]
  (prn (str "fetching " url))
  (let [elems (foo url)]
    {:kriti (song-name elems)
     :ragam (raga-name elems)
     :url url}))

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

(defn find-ragam-for-krithi [kriti]
  (let [search-result (:ragam (r/search (:ragam kriti)))]
    (merge kriti search-result)))

(defn all-song-ragas []
  (let [all-pages as
        urls (map build-url all-pages)]
    (->>  urls
          (map song-raga)
          (map find-ragam-for-krithi))))
