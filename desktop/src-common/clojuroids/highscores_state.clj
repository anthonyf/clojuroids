(ns clojuroids.highscores-state
  (:require [clojuroids.game-state-manager :as gsm]
            [clojuroids.common :as c]
            [clojuroids.key-state :as k]
            [clojuroids.game-data :as gd])
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch GlyphLayout]
           [com.badlogic.gdx.graphics Color]
           [com.badlogic.gdx Input$Keys]))

(declare handle-input)

(defrecord HighscoresState [sprite-batch font highscores glyph-layout]
  gsm/GameState

  (init [this]
    (merge this
           {:sprite-batch (SpriteBatch.)
            :glyph-layout (GlyphLayout.)
            :font (c/gen-font 20 Color/WHITE)
            :highscores (gd/load-highscores)}))

  (update! [this delta-time]
    (-> this
        (handle-input)))

  (draw [this]
    (.setProjectionMatrix sprite-batch (.combined @c/camera))
    (.begin sprite-batch)
    (let [title "High Scores"
          [sw _] c/screen-size]
      (.draw font sprite-batch title
             (float (/ (- sw (.width (doto glyph-layout
                                       (.setText font title))))
                       2))
             (float 300))
      (doseq [{name :name score :score} highscores
              i (range 10)]
        (let [str (format "%2d. %7s %s" (inc i) score name)]
          (.draw font sprite-batch
                 str
                 (float (/ (- sw (.width (doto glyph-layout
                                           (.setText font str))))
                           2))
                 (float (- 270 (* 20 i)))))))
    (.end sprite-batch))

  (dispose [this]
    (.dispose sprite-batch)))

(defn handle-input [this]
  (when (or (k/key-pressed? Input$Keys/ENTER)
            (k/key-pressed? Input$Keys/ESCAPE))
    (gsm/set-state! :menu))
  this)

(defn make-highscores-state
  []
  (map->HighscoresState {}))
