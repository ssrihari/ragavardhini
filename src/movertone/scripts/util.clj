(ns movertone.scripts.util
  (:require [clojure.java.io :as io]
            [net.cgrand.enlive-html :as html]))

(defn str->html-resource [string]
  (let [filename (str (gensym "html-resource") ".html")]
    (spit filename string)
    (-> filename
        io/file
        html/html-resource)))
