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

(defn composer-name [elems]
  (->> (html/select elems [:p :a])
       (filter #(re-find #"co\d+.*" (get-in % [:attrs :href])))
       first
       :content
       first))

(defn song-raga [url]
  (prn (str "fetching " url))
  (try
    (let [elems (foo url)]
      {:kriti (song-name elems)
       :ragam (raga-name elems)
       :composer (composer-name elems)
       :url url})
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
    (->>  urls
          (pmap song-raga))))

(defn all-kritis []
  (let [data (doall (all-song-ragas))
        filename "all-karnatic-kritis.edn"]
    (spit (-> filename io/resource)
          (-> data pp/pprint with-out-str))))

(def kritis
  (r/read-file "all-karnatic-kritis.edn"))

(defn find-ragam-for-krithi [kriti]
  (let [search-result (:ragam (r/search (:ragam kriti)))]
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
