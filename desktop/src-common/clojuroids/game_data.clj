(ns clojuroids.game-data)

(def save-file "~/.clojuroids")

(defn load-highscores
  []
  (try
    (binding [*read-eval* false]
      (read-string (slurp save-file)))
    (catch Exception e
      (map (fn [_]
             {:name "---" :score 0})
           (range 10)))))

(defn save-highscores
  [highscores]
  (spit save-file
        (with-out-str
          (pr highscores))))

(defn add-high-score
  [scores name score]
  (-> scores
      (conj {:name name :score score})
      (->> (sort-by :score >)
           (take 10))
      (save-highscores)))
