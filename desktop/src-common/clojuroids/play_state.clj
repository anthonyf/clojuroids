(ns clojuroids.play-state
  (:require [clojuroids.game-state-manager :as gsm]
            [clojuroids.player :as p]
            [clojuroids.key-state :as ks])
  (:import [com.badlogic.gdx.graphics.glutils ShapeRenderer]
           (com.badlogic.gdx Input$Keys))
  (:gen-class))

(defrecord PlayState [screen-size-ref key-state-ref shape-renderer player]
  gsm/game-state
  (init [this]
    (merge this
           {:shape-renderer (ShapeRenderer.)
            :player (p/make-player @screen-size-ref)}))

  (update! [this screen-size delta-time]
    (update this :player #(p/update-player! % screen-size delta-time)))

  (draw [this]
    (p/draw-player player shape-renderer))

  (handle-input [this]
    (assoc this :player (-> player
                            (assoc :left? (ks/key-down? @key-state-ref Input$Keys/LEFT))
                            (assoc :right? (ks/key-down? @key-state-ref Input$Keys/RIGHT))
                            (assoc :up? (ks/key-down? @key-state-ref Input$Keys/UP)))))
  (dispose [this]))

(defn make-play-state
  [screen-size-ref key-state-ref]
  (map->PlayState {:screen-size-ref screen-size-ref
                   :key-state-ref key-state-ref}))
