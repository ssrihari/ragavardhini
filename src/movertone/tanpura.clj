(ns movertone.tanpura
  (:use overtone.core))

(defn play [note amp]
  ((sample
    (case note
      60 "resources/electronic-tanpura/155483__sankalp__electronic-tanpura-1.wav"))
   :amp amp))
