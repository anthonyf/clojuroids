(ns clojuroids.highscores-state
  (:require [clojuroids.game-state-manager :as gsm]
            [clojuroids.common :as c]
            [clojuroids.key-state :as k])
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch]
           [com.badlogic.gdx.graphics Color]
           [com.badlogic.gdx Input$Keys]))

(declare handle-input)

(defrecord HighscoresState [sprite-batch font highscores]
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
    (.draw font sprite-batch "highscores" (float 100) (float 100))
    (.end sprite-batch))

  (dispose [this]))

(defn handle-input [this]
  (when (or (k/key-pressed? Input$Keys/ENTER)
            (k/key-pressed? Input$Keys/ESCAPE))
    (gsm/set-state! :menu))
  this)

(defn make-highscores-state
  []
  (map->HighscoresState {}))
