(ns movertone.scripts.samples
  (:use movertone.scripts.frequencies))


(comment
  (def sh (->perc-histogram (swaram-histogram [{:f "Jagadanandakaraka.mp3.wav.pitch.frequencies"}]))))

(def bauli-files
  ["bauLi-BV-Raman,BV-Lakshmanan-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-Chinmaya-Sisters---Uma,Radhika-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-Gayathri-Venkataraghavan-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-KL-Sriram-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-KS-Rukmani-Venkatachalam-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-Multiple-Artists-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-Nanditha-Ravi-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-Palghat-KV-Narayanaswamy-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-R-Ganesh-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-Saranya-Krishnan-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-Seetha-Rajan-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-Sethalapathi-Balasubramaniam-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-Subashree-Mani-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-Subha-Ganesan-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-Suguna-Purushothaman-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-Trichur-V-Ramachandran-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-Visalakshi-Nithyanand-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"
   "bauLi-Vyasarpadi-G-Kothandaraman-karuNAnidhiyE_tAyE-pApanAsam_sivan.mpeg.wav.pitch.frequencies"])

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

(def tracks ["valaji-varnam-bombay-jayashree.mp3.wav.pitch.frequencies"
             "04-sogasu_jUDa_taramA-kannaDagauLa.mp3.wav.pitch.frequencies"
             {:f "Jagadanandakaraka.mp3.wav.pitch.frequencies"}])

(def pancharatna-kritis
  ["Jagadanandakaraka.mp3.wav.pitch.frequencies"
   "Dudukugala.mp3.wav.pitch.frequencies"
   "Sadhinchane.mp3.wav.pitch.frequencies"
   "KanaKanaRuchira.mp3.wav.pitch.frequencies"
   "Endaro.mp3.wav.pitch.frequencies"])
