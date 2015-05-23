(ns movertone.db
  (:require [movertone.ragams :as r]
            [clojure.string :as str]
            [clojure.java.jdbc :as j]))

(defn ragam->csv [[raga-name {:keys [num parent-mela-num arohanam avarohanam]}]]
  (str (str/join "|" [(name raga-name)
                      num
                      (str/join "," (map name arohanam))
                      (str/join "," (map name avarohanam))
                      parent-mela-num])
       "\n"))

(defn ragams->csv []
  (->> r/ragams
       (mapv ragam->csv)
       (apply str)
       (spit "ragams.psv")))

(def db-spec {:subprotocol "postgresql"
              :subname "//127.0.0.1:5432/ragas"
              :user "sriharisriraman"
              :password ""})

(defn db-ragam->ragam [db-ragam]
  (r/ragams (keyword (:name db-ragam))))

(defn build-result [ragams perc]
  {:ragam (first ragams)
   :more (rest ragams)
   :perc (format "%.1f" perc)})

(defn search-ragam
  ([ragam] (search-ragam ragam 0.9))
  ([ragam perc]
     (let [q "select * from ragams where name % ? and similarity (name, ?) > ?;"
           res (j/query db-spec [q ragam ragam perc])
           ragams (mapv db-ragam->ragam res)]
       (if (seq ragams)
         (build-result ragams perc)
         (search-ragam ragam (- perc 0.1))))))

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
