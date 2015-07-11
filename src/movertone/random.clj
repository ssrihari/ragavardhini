(ns movertone.random
  (:require [movertone.core :as c]
            [movertone.swarams :as sw]
            [movertone.ragams :as r]))

(defn adjacent-differences [nums]
  (loop [num-list nums
         res []]
    (let [two-nums (take 2 num-list)]
      (if (= 2 (count two-nums))
        (recur (rest num-list)
               (conj res (Math/abs (apply - two-nums))))
        res))))

(defn gen-durations []
  (let [m 16
        n 8
        sum (- m n)]
    (->> (repeatedly n #(rand-nth (range (inc sum))))
         (cons sum)
         sort
         adjacent-differences
         (map inc))))

(defn random-swarams [swarams num]
  (repeatedly num #(rand-nth swarams)))

(defn random-durations [jathi num]
  ;; [1, 2, 1, 1, 4, 1, 1, 2, 2, 4]
  (take num (flatten (repeatedly gen-durations))))

(defn random-phrase [ragam-name jathi num]
  (let [ragam (ragam-name r/ragams)
        swarams-in-ragam (sw/actual-swarams->simple-swarams
                          (sw/madhya-swarams-in-ragam ragam))]
    (c/phrase ragam
              (vec (random-swarams swarams-in-ragam num))
              (vec (random-durations jathi num))
              (:lower c/kalams))))

(defn play-random-phrase [ragam-name jathi num]
  (c/play-phrase (random-phrase ragam-name jathi num)))

(comment
  (play-random-phrase :vasanti 3 100))
