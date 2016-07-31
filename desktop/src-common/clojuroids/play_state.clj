(ns clojuroids.play-state
  (:require [clojuroids.game-state-manager :as gsm]
            [clojuroids.player :as p]
            [clojuroids.bullet :as b]
            [clojuroids.asteroids :as a]
            [clojuroids.key-state :as ks])
  (:import [com.badlogic.gdx.graphics.glutils ShapeRenderer]
           [com.badlogic.gdx.math MathUtils]
           (com.badlogic.gdx Input$Keys))
  (:gen-class))

(defn- random-asteroid-location
  [screen-size player-pos]
  (let [[screen-width screen-height] screen-size
        [px py] player-pos
        [ax ay] [(MathUtils/random screen-width)
                 (MathUtils/random screen-height)]
        [dx dy] [(- ax px)
                 (- ay py)]
        dist (Math/sqrt (+ (* dx dx) (* dy dy)))]
    (if (< dist 100)
      (recur screen-size player-pos)
      [ax ay])))

(defn- spawn-asteroids
  [state]
  (let [{:keys [level player screen-size-ref]} state
        player-pos (get-in player [:space-object :pos])
        num-to-spawn (+ 4 (- level 1))
        total-asteroids (* num-to-spawn 7)
        asteroids (for [_ (range num-to-spawn)]
                    (a/make-asteroid (random-asteroid-location @screen-size-ref player-pos)
                                     :large))]
    (merge state
           {:asteroids asteroids
            :total-asteroids total-asteroids
            :num-asteroids-left total-asteroids})))

(defrecord PlayState [screen-size-ref key-state-ref shape-renderer player
                      bullets asteroids
                      level total-asteroids num-asteroids-left]
  gsm/game-state
  (init [this]
    (-> this
        (merge {:shape-renderer (ShapeRenderer.)
                :player         (p/make-player @screen-size-ref)
                :level          1})
        (spawn-asteroids)))

  (update! [this screen-size delta-time]
    (-> this
        (update :player #(p/update-player! % screen-size delta-time))
        (update :bullets #(b/update-bullets % screen-size delta-time))
        (update :asteroids #(a/update-asteroids % screen-size delta-time))))

  (draw [this]
    (p/draw-player player shape-renderer)
    (b/draw-bullets bullets shape-renderer)
    (a/draw-asteroids asteroids shape-renderer))

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
