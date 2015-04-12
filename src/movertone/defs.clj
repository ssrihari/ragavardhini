(ns movertone.defs
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [medley.core :as m]))

(defn read-file [filename]
  (-> filename io/resource slurp edn/read-string))

(def shruthis
  {:.a  57
   :.a# 58
   :.b  59
   :c   60
   :c#  61
   :db  61
   :d   62
   :d#  63
   :eb  63
   :e   64
   :f   65
   :f#  66
   :gb  66
   :g   67
   :g#  68
   :ab  68
   :a   69
   :a#  70
   :bb  70
   :b   71
   :c.  72})

(def swarams->names
  {:s  "Shadjamam"
   :r1 "Sudhdha Rishabam"
   :r2 "Chatusruthi Rishabam"
   :r3 "Shatsruthi Rishabam"
   :g1 "Sudhdha Gaandhaaram"
   :g2 "Saadhaarana Gaandhaaram"
   :g3 "Anthara Gaandhaaram"
   :m1 "Sudhdha Madhyamam"
   :m2 "Prathi Madhyamam"
   :p  "Panchamam"
   :d1 "Sudhdha Dhaivatham"
   :d2 "Chatusruthi Dhaivatham"
   :d3 "Shatsruthi Dhaivatham"
   :n1 "Sudhdha Nishaadham"
   :n2 "Kaisika Nishaadham"
   :n3 "Kaakali Nishaadham"})

(def madhya-sthayi
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
   :n3 11})

(def sthayis
  {:anumandra {:position :before :dots ".." :difference -24}
   :mandra    {:position :before :dots "."  :difference -12}
   :madhya    {:position :none   :dots ""   :difference 0}
   :thara     {:position :after  :dots "."  :difference 12}
   :athithara {:position :after  :dots ".." :difference 24}})

(defn swara-rep->sthayi [swaram {:keys [position dots]}]
  (condp = position
    :before
    (->> swaram name (str dots) keyword)

    :after
    (-> swaram name (str dots) keyword)

    :none
    swaram))

(defn swaram->sthayi [{:keys [position dots difference] :as sthayi} [swaram sthanam]]
  (let [new-rep (swara-rep->sthayi swaram sthayi)
        new-sthanam (+ difference sthanam)]
    {new-rep new-sthanam}))

(defn ->sthayi [sthayi]
  (->> madhya-sthayi
       (map (partial swaram->sthayi sthayi))
       (into {})))

(def swarams->notes
  (apply merge (map #(->sthayi %) (vals sthayis))))

(defn raga [{:keys [arohanam avarohanam] :as scale}]
  (zipmap arohanam scale))

(defn melakartha-swarams->ragam [swarams]
  {:arohanam swarams :avarohanam (reverse swarams)})

(def melakarthas
  (->> (read-file "ragas.edn")
       :melakarthas
       (m/map-vals melakartha-swarams->ragam)))

(def janyas-of-melakarthas
  (read-file "more-ragas.edn"))

(def janyas
  (->> janyas-of-melakarthas
       vals
       (apply concat)
       (into {})))

(def simple-swarams
  [:s :r :g :m :p :d :n])

(defn madhya-swarams-in-ragam [{:keys [arohanam avarohanam]}]
  (let [swarams (concat arohanam avarohanam)]
    (->> swarams
         (remove #(.contains (name %) "."))
         set)))

(defn simple-swaram->actual-swaram [simple-swaram madhya-swarams]
  (first
   (filter #(.contains (name %) (name simple-swaram))
           madhya-swarams)))

(defn simple-swarams->actual-swarams [{:keys [arohanam avarohanam] :as ragam}]
  (let [madhya-swarams (madhya-swarams-in-ragam ragam)]
    (into {}
          (keep (fn [simple-swaram]
                  (when-let [actual-swaram
                             (simple-swaram->actual-swaram simple-swaram madhya-swarams)]
                    [simple-swaram actual-swaram]))
                simple-swarams))))

(defn get-simple-swaram-mappings [ragam]
  (into {}
        (for [[sim act] (simple-swarams->actual-swarams ragam)
              sthayi (vals sthayis)]
          [(swara-rep->sthayi sim sthayi)
           (swara-rep->sthayi act sthayi)])))
