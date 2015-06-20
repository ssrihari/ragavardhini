(ns movertone.db
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as j]))

(defn ragam->csv [[raga-name {:keys [num parent-mela-num arohanam avarohanam]}]]
  (str (str/join "|" [(name raga-name)
                      num
                      (str/join "," (map name arohanam))
                      (str/join "," (map name avarohanam))
                      parent-mela-num])
       "\n"))

(defn kriti->csv [{:keys [kriti ragam composer url]}]
  (str (str/join "|" [(name ragam)
                      kriti
                      composer
                      url])
       "\n"))

(defn ragams->csv [ragams]
  (->> ragams
       (mapv ragam->csv)
       (apply str)
       (spit "ragams.psv")))

(defn kritis->csv [kritis]
  (->> kritis
       (mapv kriti->csv)
       (apply str)
       (spit "kritis.psv")))

(def db-spec {:subprotocol "postgresql"
              :subname "//127.0.0.1:5432/ragas"
              :user "sriharisriraman"
              :password ""})

(defn search-ragam [ragam perc]
  (let [q "SELECT *,
           difference (?, name) AS soundex_diff,
           levenshtein (?, name) AS lev_diff
           FROM ragams
           WHERE name % ? AND similarity (name, ?) > ?
           ORDER BY lev_diff, soundex_diff desc"]
    (j/query db-spec [q ragam ragam ragam ragam perc])))


(defn search-kriti [kriti]
  (let [q " SELECT *,
            difference (?, name) AS soundex_diff,
            levenshtein (?::text, name) AS lev_diff
            FROM kritis
            WHERE name != ''
            ORDER BY soundex_diff DESC, lev_diff, url
            LIMIT 10;"]
    (j/query db-spec [q kriti kriti])))

;; setup DB
;;-----------------------
;; initdb -D data
;; pg_ctl -D data -l pg.log start

;; create table
;;-----------------------
;; create table ragams (
;; id bigserial primary key,
;; mela_num bigint unique,
;; name varchar(100),
;; arohanam varchar(50),
;; avarohanam varchar(50),
;; parent_mela_num bigint references ragams(mela_num));

;; create table kritis (
;;   id bigserial primary key,
;;   name varchar(100),
;;   composer varchar(50),
;;   url varchar(200),
;;   ragam varchar(100),
;;   ragam_id bigint references ragams(id)
;;   );

;; populate db
;;-----------------------
;; (ragams->csv)
;; \copy ragams (name, mela_num, arohanam, avarohanam, parent_mela_num) from ragams.psv csv delimiter '|';
;; \copy kritis (ragam, name,composer,url) from kritis.psv csv delimiter '|';
;; update kritis k set ragam_id = r.id from ragams r where k.ragam = r.name;

;; prepare search
;;-----------------------
 ;; http://bartlettpublishing.com/site/bartpub/blog/3/entry/350
 ;; CREATE EXTENSION pg_trgm;
 ;; create extension fuzzystrmatch;
 ;; CREATE INDEX ragas_name_trigram_idx ON ragams USING gist(name gist_trgm_ops);
 ;; CREATE INDEX kritis_name_trigram_idx ON kritis USING gist(name gist_trgm_ops);

;; prepare query
;;-----------------------
;; prepare search (varchar, float) AS select * from ragas where name % $1 and similarity (name, $1) > $2;

;; usage
;;-----------------------
;; execute search('abhogi', 0.4);
