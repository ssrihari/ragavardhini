(ns movertone.scripts.sangeetha-priya-scraper
  (:require [clojure.string :as s]
            [clojure.pprint :as pp]
            [net.cgrand.enlive-html :as html]
            [clj-http.client :as client]
            [clojure.java.io :as io]))

(defn str->html-resource [string]
  (let [filename (str (gensym "html-resource") ".html")]
    (spit filename string)
    (-> filename
        io/file
        html/html-resource)))

(defn pretty-name [kriti-name]
  (->> (s/split kriti-name #"_")
       (map s/capitalize)
       (s/join " ")))

(defn row->entity [row]
  (let [row-keys [:concert :track :kriti :ragam :composer :artist :null]
        req-keys [:kriti :ragam :composer]
        entity (select-keys (zipmap row-keys row)
                            req-keys)]
    (assoc entity
      :kriti (pretty-name (:kriti entity))
      :composer (pretty-name (:composer entity)))))

(defn kritis [ragam]
  (prn "fetching kritis for" ragam)
  (try
    (let [ragam-post-url "http://www.sangeethapriya.org/fetch_tracks.php?ragam"
          res (str->html-resource (client/post ragam-post-url {:form-params {"FIELD_TYPE" ragam}}))
          cells (html/select res [:td])
          rows (partition 7 (map html/text cells))
          entities (map row->entity rows)]
      (distinct entities))
    (catch Exception e
      (prn "Caught exception when fetching kritis for " ragam)
      (.printStackTrace e)
      nil)))

(defn get-all-raga-names []
  (prn "fetching all raga names")
  (let [url "http://www.sangeethapriya.org/display_tracks.php"
        res (str->html-resource (client/get url))
        selects (html/select res [:form :td :select])
        ragas (nth selects 5)]
    (map html/text (html/select ragas [:option]))))

(defn all-kritis []
  (let [data (doall (into [] (pmap kritis (get-all-raga-names))))
        filename "all-kritis.edn"]
    (spit (-> filename io/resource)
          (-> data pp/pprint with-out-str))))
