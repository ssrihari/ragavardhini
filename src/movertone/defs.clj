(ns movertone.defs
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [medley.core :as m]))

(defn read-file [filename]
  (-> filename io/resource slurp edn/read-string))

(def swarams->names
  {:s "Shadjamam"
   :r1 "Sudhdha Rishabam"
   :r2 "Chatusruthi Rishabam"
   :r3 "Shatsruthi Rishabam"
   :g1 "Sudhdha Gaandhaaram"
   :g2 "Saadhaarana Gaandhaaram"
   :g3 "Anthara Gaandhaaram"
   :m1 "Sudhdha Madhyamam"
   :m2 "Prathi Madhyamam"
   :p "Panchamam"
   :d1 "Sudhdha Dhaivatham"
   :d2 "Chatusruthi Dhaivatham"
   :d3 "Shatsruthi Dhaivatham"
   :n1 "Sudhdha Nishaadham"
   :n2 "Kaisika Nishaadham"
   :n3 "Kaakali Nishaadham"})

(def swarams->notes
  {:s 0
   :r1 1
   :r2 2
   :r3 3
   :g1 2
   :g2 3
   :g3 4
   :m1 5
   :m2 6
   :p 7
   :d1 8
   :d2 9
   :d3 10
   :n1 9
   :n2 10
   :n3 11
   :s2 12})

(def arohanam
  [:s :r :g :m :p :d :n :s2])

(def avarohanam
  (reverse arohanam))

(def arohanam+avarohanam
  (concat arohanam avarohanam))

(defn scale->ragam [scale]
  (zipmap arohanam scale))

(def melakarthas
  (->> (read-file "ragas.edn")
       :melakarthas
       (m/map-vals scale->ragam)))

;; get these ragas from a file
(def shankarabharanam
  {:s :s
   :r :r2
   :g :g3
   :m :m1
   :p :p
   :d :d2
   :n :n3
   :s2 :s2})

(def mayamalavagowlai
  {:s :s
   :r :r1
   :g :g3
   :m :m1
   :p :p
   :d :d1
   :n :n3
   :s2 :s2})
