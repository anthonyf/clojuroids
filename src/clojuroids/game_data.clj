(ns clojuroids.game-data
  (:require [clojure.java.io :as io]))

(def save-file (io/file (System/getProperty "user.home")
                        ".clojuroids"))

(defn- load-high-scores
  []
  (try
    (binding [*read-eval* false]
      (read-string (slurp save-file)))
    (catch Exception e
      (map (fn [_]
             {:name "---" :score 0})
           (range 10)))))

(def high-scores (atom (load-high-scores)))

(defn- save-high-scores
  [high-scores]
  (spit save-file
        (with-out-str
          (pr high-scores)))
  high-scores)

(defn add-high-score
  [name score]
  (reset! high-scores
          (-> @high-scores
              (conj {:name name :score score})
              (->> (sort-by :score >)
                   (take 10))
              save-high-scores)))

(defn high-score?
  [score]
  (-> @high-scores
      last
      :score
      (< score)))
