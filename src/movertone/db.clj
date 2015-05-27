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

(defn ragams->csv [ragams]
  (->> ragams
       (mapv ragam->csv)
       (apply str)
       (spit "ragams.psv")))

(def db-spec {:subprotocol "postgresql"
              :subname "//127.0.0.1:5432/ragas"
              :user "sriharisriraman"
              :password ""})

(defn search [ragam perc]
  (let [q "SELECT *,
           difference (?, name) AS soundex_diff,
           levenshtein (?, name) AS lev_diff
           FROM ragams
           WHERE name % ? AND similarity (name, ?) > ?
           ORDER BY lev_diff, soundex_diff desc"]
    (j/query db-spec [q ragam ragam ragam ragam perc])))

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

;; populate db
;;-----------------------
;; (ragams->csv)
;; \copy ragams (name, mela_num, arohanam, avarohanam, parent_mela_num) from ragams.psv csv delimiter '|';

;; prepare search
;;-----------------------
;; http://bartlettpublishing.com/site/bartpub/blog/3/entry/350
;; CREATE EXTENSION pg_trgm;
;; CREATE INDEX ragas_name_trigram_idx ON ragas USING gist(name gist_trgm_ops);

;; prepare query
;;-----------------------
;; prepare search (varchar, float) AS select * from ragas where name % $1 and similarity (name, $1) > $2;

;; usage
;;-----------------------
;; execute search('abhogi', 0.4);
