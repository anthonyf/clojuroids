(ns clojuroids.play-state
  (:require [clojuroids.game-state-manager :as gsm]
            [clojuroids.player :as p]
            [clojuroids.bullet :as b]
            [clojuroids.key-state :as ks])
  (:import [com.badlogic.gdx.graphics.glutils ShapeRenderer]
           (com.badlogic.gdx Input$Keys))
  (:gen-class))

(defrecord PlayState [screen-size-ref key-state-ref shape-renderer player
                      bullets]
  gsm/game-state
  (init [this]
    (merge this
           {:shape-renderer (ShapeRenderer.)
            :player (p/make-player @screen-size-ref)}))

  (update! [this screen-size delta-time]
    (-> this
        (update :player #(p/update-player! % screen-size delta-time))
        (update :bullets #(b/update-bullets % screen-size delta-time))))

  (draw [this]
    (p/draw-player player shape-renderer)
    (b/draw-bullets bullets shape-renderer))

  (handle-input [this]
    (as-> this state
      (assoc state :player (-> player
                               (assoc :left? (ks/key-down? @key-state-ref Input$Keys/LEFT))
                               (assoc :right? (ks/key-down? @key-state-ref Input$Keys/RIGHT))
                               (assoc :up? (ks/key-down? @key-state-ref Input$Keys/UP))))
      (assoc state :bullets (if (ks/key-pressed? @key-state-ref Input$Keys/SPACE)
                              (let [{:keys [player bullets]} state]
                                (b/shoot player bullets))
                              bullets))))
  (dispose [this]))

(defn make-play-state
  [screen-size-ref key-state-ref]
  (map->PlayState {:screen-size-ref screen-size-ref
                   :key-state-ref key-state-ref}))
