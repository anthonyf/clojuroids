(ns clojuroids.play-state
  (:require [clojuroids.common :as c]
            [clojuroids.game-state-manager :as gsm]
            [clojuroids.player :as p]
            [clojuroids.key-state :as ks])
  (:import [com.badlogic.gdx.graphics.glutils ShapeRenderer]
           (com.badlogic.gdx Input$Keys)))

(defrecord PlayState [shape-renderer player]
  gsm/GameState
  (init [this]
    (let [[w h] c/screen-size]
      (merge this
             {:shape-renderer (ShapeRenderer.)
              :player (p/make-player :pos [(/ w 2)(/ h 2)])})))

  (update! [this delta-time]
    (update this :player p/update-player! delta-time))

  (draw [this]
    (p/draw-player player shape-renderer))

  (dispose [this]
    (.dispose shape-renderer)))

(defn make-play-state
  []
  (map->PlayState {}))
