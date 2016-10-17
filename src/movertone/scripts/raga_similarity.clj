(ns movertone.scripts.raga-similarity
  (:use [incanter core stats charts io datasets])
  (:require [overtone.core :as o]
            [clojure.string :as s]
            [movertone.swarams :as sw]
            [movertone.scripts.frequencies :as f]
            [movertone.scripts.samples :as samples]))

(defn nil-plus [n1 n2]
  (if (nil? n1)
    n2
    (+ n1 n2)))

(defn normalize-octaves [osp]
  (reduce (fn [x [swaram occurrances]]
            (update-in x [(mod (sw/swarams->notes swaram) 12)] nil-plus occurrances))
          {}
          osp))

(defn normalized-one-swaram-probabilities [files]
  (->> (pmap #(f/one-swaram-probabilities-for-file % {:tonic-prominence 1}) files)
       (apply merge-with +)
       (#(dissoc % nil))
       normalize-octaves
       f/->perc-histogram))

(defn diff [base-osp sample-osp]
  (let [ordered-vals (fn [osp] (vals (sort-by first < osp)))
        probs (ordered-vals base-osp)
        table (ordered-vals sample-osp)
        data-set (to-dataset (map (fn [sw bo so] {:swaram sw
                                                  :base-ocurrance bo
                                                  :sample-ocurrance so})
                                  (range 12) probs table))]
    (with-data data-set
      (save (-> (bar-chart :swaram
                           :sample-ocurrance
                           :series-label "sample"
                           :title (str "raga diff")
                           :legend true)
                (add-categories :swaram
                                :base-ocurrance
                                :series-label "base"))
            (str (gensym "raga-diff") ".png")
            :width 1200
            :height 300))
    (chisq-test :table table
                :probs (map #(* 0.01 %) probs))))

(comment
  (def mohanam-base (normalized-one-swaram-probabilities samples/mohanam-files))
  (def kalyani-base (normalized-one-swaram-probabilities samples/kalyani-files))
  (def revati-base (normalized-one-swaram-probabilities samples/revati-files))

  (for [base ['mohanam-base
              'kalyani-base
              'revati-base]
        sample-collection ['samples/mohanam-files
                           'samples/kalyani-files
                           'samples/revati-files]]
    (do (prn :base base
             :sample-collection sample-collection)
        (clojure.pprint/pprint
         (for [file (take 10 (eval sample-collection))]
           (:X-sq (diff (eval base)
                        (normalized-one-swaram-probabilities [file])))))))
  )
