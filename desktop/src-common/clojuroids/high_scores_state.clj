(ns clojuroids.high-scores-state
  (:require [clojuroids.game-state-manager :as gsm]
            [clojuroids.common :as c]
            [clojuroids.key-state :as k]
            [clojuroids.game-data :as gd])
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch]
           [com.badlogic.gdx.graphics Color]
           [com.badlogic.gdx Input$Keys]))

(declare handle-input)

(defrecord HighScoresState [sprite-batch font]
  gsm/GameState

  (init [this]
    (merge this
           {:sprite-batch (SpriteBatch.)
            :font (c/gen-font 20 Color/WHITE)}))

  (update! [this delta-time]
    (-> this
        (handle-input)))

  (draw [this]
    (.setProjectionMatrix sprite-batch (.combined @c/camera))
    (.begin sprite-batch)
    (let [title "High Scores"]
      (c/draw-centered-text sprite-batch font title 300)
      (doseq [[{name :name score :score} i]
              (map list @gd/high-scores (range 10))]
        (let [str (format "%2d. %7s %s" (inc i) score name)]
          (c/draw-centered-text sprite-batch font str (- 270 (* 20 i))))))
    (.end sprite-batch))

  (dispose [this]
    (.dispose sprite-batch)
    (.dispose font)))

(defn handle-input [this]
  (when (or (k/key-pressed? Input$Keys/ENTER)
            (k/key-pressed? Input$Keys/ESCAPE))
    (gsm/set-state! :menu))
  this)

(defn make-high-scores-state
  []
  (map->HighScoresState {}))
