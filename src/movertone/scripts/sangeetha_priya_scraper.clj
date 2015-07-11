(ns movertone.scripts.sangeetha-priya-scraper
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as s]
            [medley.core :as m]
            [movertone.ragams :as r]
            [movertone.search :as search]
            [movertone.scripts.util :as util]
            [net.cgrand.enlive-html :as html]))

(defn my-pmap [f coll n]
  (let [rets (map #(future (f %)) coll)
        step (fn step [[x & xs :as vs] fs]
               (lazy-seq
                (if-let [s (seq fs)]
                  (cons (deref x) (step xs (rest s)))
                  (map deref vs))))]
    (step rets (drop n rets))))

(defonce row-counter (atom 0))
(defonce error-counter (atom 0))

(defn reset-counters! []
  (reset! row-counter 0)
  (reset! error-counter 0))

(defn pretty-name [kriti-name]
  (->> (s/split kriti-name #"_")
       (map s/capitalize)
       (s/join " ")))

(def cookies
  [["PHPSESSID" {:domain "www.sangeethamshare.org" :value "2kp68nggipf3pieo96r0jnvm50"}]
   ["G_ENABLED_IDPS" {:domain ".www.sangeethamshare.org" :value "google"}]
   ["_ga" {:domain ".sangeethamshare.org" :value "GA1.2.1679019818.1435463904"}]
   ["_gat" {:domain ".sangeethamshare.org" :value "1"}]
   ["PHPSESSID" {:domain "www.sangeethapriya.org" :value "6f99q5l9cusv4v7pcjvd3pleg3"}]
   ["G_ENABLED_IDPS" {:domain ".www.sangeethapriya.org" :value "google"}]
   ["G_AUTHUSER_H" {:domain ".www.sangeethapriya.org" :value "0"}]
   ["_ga" {:domain ".sangeethapriya.org" :value "GA1.2.1035688716.1435464084"}]
   ["_gat" {:domain ".sangeethapriya.org" :value "1"}]])

(defn build-kriti-audio-link [album-url track-num sangethapriya-kriti-name]
  (try
    (let [track-num (Long/parseLong track-num)
          response (util/str->html-resource (client/get album-url {:cookies cookies}))
          mirror-2-ext (-> (html/select response [:a.download])
                           (nth (dec track-num))
                           (get-in [:attrs :onmousedown])
                           (s/split #",")
                           (nth 2)
                           (s/replace #"[\\\"]" ""))
          mirror-2-base-url "http://sangeethapriya.gotdns.org:8080/cgi-bin/download.cgi?"]
      (when mirror-2-ext
        (str mirror-2-base-url mirror-2-ext)))
    (catch Exception e
      (prn e)
      (swap! error-counter inc)
      (prn "got exception while building audio link for " sangethapriya-kriti-name " from album " album-url)
      nil)))

(defn row->entity [row]
  (swap! row-counter inc)
  (let [row-text (map html/text row)
        album-url (some-> row first :content first :attrs :href)
        row-keys [:concert :track-num :kriti :ragam :composer :artist :null]
        req-keys [:kriti :ragam :composer :artist :track-num]
        entity (select-keys (zipmap row-keys row-text)
                            req-keys)
        sangethapriya-kriti-name (:kriti entity)
        rendition-url (build-kriti-audio-link album-url (:track-num entity)
                                              sangethapriya-kriti-name)]
    (prn (str sangethapriya-kriti-name " by " (:artist entity)))
    (assoc entity
      :sangethapriya-kriti-name sangethapriya-kriti-name
      :album-url album-url
      :rendition-url rendition-url
      :kriti (pretty-name (:kriti entity))
      :composer (pretty-name (:composer entity))
      :artist (pretty-name (:artist entity)))))

(defn kritis [ragam]
  (prn "fetching kritis for" ragam)
  (try
    (let [ragam-post-url "http://www.sangeethapriya.org/fetch_tracks.php?ragam"
          res (util/str->html-resource (client/post ragam-post-url {:form-params {"FIELD_TYPE" ragam}}))
          cells (html/select res [:td])
          rows (partition 7 cells)
          entities (doall (my-pmap row->entity rows 20))
          filename "all-kritis.edn"]
      (distinct entities)
      (for [entity entities]
        (spit (-> filename io/resource)
              (s/join "|" (map entity [:kriti :ragam :composer :artist :rendition-url :album-url :sangethapriya-kriti-name]))
              :append true)))
    (catch Exception e
      (def *ex e)
      (prn "Caught exception when fetching kritis for " ragam)
      (.printStackTrace e)
      nil)))

(defn get-all-raga-names []
  (prn "fetching all raga names")
  (let [url "http://www.sangeethapriya.org/display_tracks.php"
        res (util/str->html-resource (client/get url))
        selects (html/select res [:form :td :select])
        ragas (nth selects 5)]
    (map html/text (html/select ragas [:option]))))

(defn all-kritis []
  (reset-counters!)
  (let [data (doall (into [] (my-pmap kritis (take 2 (get-all-raga-names)) 30)))]
    (def *d data)))

(def kritis-var
  (r/read-file "all-kritis.edn"))

(defn find-ragam-for-krithi [kriti]
  (let [search-result (:ragam (search/search-ragam (:ragam kriti)))]
    (merge kriti search-result)))

(defn get-name->kritis []
  (let [name->kritis (group-by :name (map find-ragam-for-krithi kritis))
        n->ks (m/map-vals (fn [ks] (map #(select-keys % [:kriti :composer]) ks))
                          name->kritis)]
    (r/write-file "raga-to-kritis.edn" n->ks)))
