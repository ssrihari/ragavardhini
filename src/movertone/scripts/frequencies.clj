(ns movertone.scripts.frequencies
  (:require [clojure.string :as s]
            [clojure.pprint :refer [pprint]]
            [medley.core :as m]
            [overtone.core :as o]
            [movertone.swarams :as sw]))

(def exp-dir
  "/Users/sriharisriraman/dev/clojure/ragavardhini/resources/exp/")

(def kosha-dir
  "/Users/sriharisriraman/dev/clojure/kosha/downloads/")

(def track-dir
  exp-dir)

(def midis
  (range 100))

(def std-freqs
  (map o/midi->hz midis))

(def default-tonic-perc 0.8)

(defn read-freqs-from-file [file]
  (->> (s/split (slurp file) #"\n")
       (remove #{"0"})
       (map #(Double/parseDouble %))))

(defn freqs->midi-histogram [freqs]
  (frequencies (map o/hz->midi freqs)))

(defn ->perc-histogram [hist]
  (let [total (reduce + (vals hist))]
    (m/map-vals #(* 100.0 (/ % total))
                hist)))

(defn nil-plus [n1 n2]
  (if (nil? n1)
    n2
    (+ n1 n2)))

(defn normalize-octaves [midi-occurrances]
  (reduce (fn [x [midi occurrances]]
            (update-in x [(mod midi 12)] nil-plus occurrances))
          {}
          midi-occurrances))

(defn find-tonic-midi [midi-occurrances prominent-note]
  (let [prominent-midi-diff (->>  midi-occurrances
                                  normalize-octaves
                                  (sort-by second >)
                                  first
                                  first)]
    (cond
      (= :s prominent-note) (+ 60 prominent-midi-diff)
      (= :p prominent-note) (+ 65 prominent-midi-diff)
      :else                 (+ 60 prominent-midi-diff))))

(defn freqs->swarams [freqs & [prominent-note]]
  (let [midi-occurrances (freqs->midi-histogram freqs)
        tonic-midi (find-tonic-midi midi-occurrances prominent-note)]
    (prn :tonic-midi tonic-midi)
    (map #(sw/midi->swaram % tonic-midi)
         (map o/hz->midi freqs))))

(defn reduce-tonic-prominence [perc hist]
  (let [tonic (ffirst (sort-by second > hist))]
    (update-in hist [tonic] #(* perc %))))

(defn remove-non-prominent-notes [perc hist]
  (->> hist
       (sort-by second >)
       (take (* perc (count hist)))
       (into {})))

(defn freqs->swarams-histogram [freqs & [prominent-note]]
  (let [midi-occurrances (freqs->midi-histogram freqs)
        tonic-midi (find-tonic-midi midi-occurrances prominent-note)]
    (prn :tonic-midi tonic-midi)
    (m/map-keys #(sw/midi->swaram % tonic-midi) (freqs->midi-histogram freqs))))

(defn swaram-histogram [files]
  (->> (for [{:keys [f prominent-note]} files
             :let [freqs (read-freqs-from-file (str track-dir "/" f))]]
         (->> (freqs->swarams-histogram freqs prominent-note)
              (reduce-tonic-prominence 0.5)))
       (apply merge-with +)
       (remove-non-prominent-notes 0.25)
       ->perc-histogram))

(defn two-swaram-freqs [freqs]
  (let [swarams (freqs->swarams freqs)]
    (frequencies
     (loop [acc []
            [f s :as all] swarams]
       (if s
         (recur (conj acc [f s]) (rest all))
         acc)))))

(defn two-swaram-histogram-for-file [file]
  (let [freqs (read-freqs-from-file (str track-dir "/" file))
        ts-freqs (group-by (fn [[[fs ss] fr]] fs)
                           (two-swaram-freqs freqs))
        prominent-swarams (set (keys (swaram-histogram [{:f file}])))]
    (->> (for [[sw t-sh] ts-freqs]
           [sw (m/map-keys second (into {} t-sh))])
         (into {})
         (m/map-vals #(reduce-tonic-prominence 0.6 %))
         (m/map-vals #(remove-non-prominent-notes 0.25 %))
         (m/filter-keys #(contains? prominent-swarams %)))))

(defn two-swaram-histogram [files]
  (->> files
       (pmap two-swaram-histogram-for-file)
       (apply merge-with #(merge-with + %1 %2))
       (m/map-vals ->perc-histogram)))

(defn pprint-histogram [hist]
  (pprint (sort-by second > hist)))

(comment
  (def sh (->perc-histogram (swaram-histogram [{:f "Jagadanandakaraka.mp3.wav.pitch.frequencies"}]))))

(def bauli-files
  [{:f "bauLi-BV-Raman,BV-Lakshmanan-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
    :prominent-note :p}
   {:f "bauLi-Chinmaya-Sisters---Uma,Radhika-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
    :prominent-note :p}
   {:f "bauLi-Gayathri-Venkataraghavan-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
    :prominent-note :s}
   {:f "bauLi-KL-Sriram-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
    :prominent-note :p}
   {:f "bauLi-Multiple-Artists-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
    :prominent-note :p}
   {:f "bauLi-Nanditha-Ravi-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"}
   {:f "bauLi-Palghat-KV-Narayanaswamy-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"}
   {:f "bauLi-R-Ganesh-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"}
   {:f "bauLi-Saranya-Krishnan-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"}
   {:f "bauLi-Seetha-Rajan-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"}
   {:f "bauLi-Sethalapathi-Balasubramaniam-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"}
   {:f "bauLi-Subashree-Mani-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"}
   {:f "bauLi-Subha-Ganesan-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"}
   {:f "bauLi-Suguna-Purushothaman-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"}
   {:f "bauLi-Trichur-V-Ramachandran-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"}
   {:f "bauLi-Visalakshi-Nithyanand-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"}
   {:f "bauLi-Vyasarpadi-G-Kothandaraman-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"}])

(def mohanam-files
  ["15mOhanam-Neyveli-Santhanagopalan-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "17mOhanam-Maharajapuram-S-Srinivasan-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "28mOhanam-Nagai-R-Muralidharan-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "28mOhanam-U-Shrinivas-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "34mOhanam-ML-Vasanthakumari-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "35mOhanam-Padma-Chandilyan-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "39mOhanam-U-Shrinivas-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "40mOhanam-Nedunuri-Krishnamurthy-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "54mOhanam-Sikkil-Mala-Chandrasekhar-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "55mOhanam-Seetha-Narayanan-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "57mOhanam-ML-Vasanthakumari-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "59mOhanam-Parur-R-Venkataraman-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "60mOhanam-U-Shrinivas-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "70mOhanam-Sangeetha-Sivakumar-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "82mOhanam-Lalgudi-Vijayalakshmi-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "82mOhanam-Sudha-Raghunathan-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "84mOhanam-Maharajapuram-S-Srinivasan-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "86mOhanam-VR-Dileepkumar-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "87mOhanam-Tadepalli-Lokanatha-Sharma-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"
   "96mOhanam-RS-Jayalakshmi-dayarAni_dayarAni_dAsharathi-tyAgarAja.mpeg.wav.pitch.frequencies"])

(comment

  (def tracks ["valaji-varnam-bombay-jayashree.mp3.wav.pitch.frequencies"
               "04-sogasu_jUDa_taramA-kannaDagauLa.mp3.wav.pitch.frequencies"
               {:f "Jagadanandakaraka.mp3.wav.pitch.frequencies"}
               "Jagadanandakaraka.mp3.wav.pitch.frequencies"
               "Dudukugala.mp3.wav.pitch.frequencies"
               "Sadhinchane.mp3.wav.pitch.frequencies"
               "KanaKanaRuchira.mp3.wav.pitch.frequencies"
               "Endaro.mp3.wav.pitch.frequencies"])

  (pprint-histogram (swaram-histogram bauli-files))
  )
