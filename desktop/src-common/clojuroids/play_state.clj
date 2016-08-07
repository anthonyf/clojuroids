(ns clojuroids.play-state
  (:require [clojure.set :as set]
            [clojuroids.game-state-manager :as gsm]
            [clojuroids.player :as p]
            [clojuroids.bullet :as b]
            [clojuroids.asteroids :as a]
            [clojuroids.key-state :as ks]
            [clojuroids.space-object :as so])
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

(def ^:const max-bullets 4)

(defn shoot
  [state]
  (let [{:keys [player bullets]} state
        {:keys [space-object]} player
        {:keys [pos radians]} space-object]
    (if (< (count bullets) max-bullets)
      (update state :bullets #(conj %
                                    (b/make-bullet pos radians)))
      state)))

(declare handle-collisions)

(defrecord PlayState [screen-size-ref key-state-ref shape-renderer player
                      bullets asteroids
                      level total-asteroids num-asteroids-left]
  gsm/game-state
  (init [this]
    (-> this
        (merge {:shape-renderer (ShapeRenderer.)
                :player         (p/make-player @screen-size-ref)
                :level          1
                :bullets        #{}
                :asteroids      #{}})
        (spawn-asteroids)))

  (update! [this screen-size delta-time]
    (let [{dead? :dead?} player]
      (if dead?
        (update this :player #(p/reset % screen-size))
        (-> this
            (update :player #(p/update-player! % screen-size delta-time))
            (update :bullets #(b/update-bullets % screen-size delta-time))
            (update :asteroids #(a/update-asteroids % screen-size delta-time))
            (handle-collisions)))))

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
      (if (ks/key-pressed? @key-state-ref Input$Keys/SPACE)
        (shoot state)
        state)))

  (dispose [this]))

(defn make-play-state
  [screen-size-ref key-state-ref]
  (map->PlayState {:screen-size-ref screen-size-ref
                   :key-state-ref key-state-ref}))

(defn- split-asteroid
  [state asteroid]
  (-> state
      (update :num-asteroids-left dec)
      (update :asteroids #(let [{type :type
                                 {:keys [pos]} :space-object} asteroid]
                            (if (contains? #{:large :medium} type)
                              (let [new-type (case type
                                               :large :medium
                                               :medium :small)]
                                (set/union % (set [(a/make-asteroid pos new-type)
                                                   (a/make-asteroid pos new-type)])))
                              %)))))

(defn- handle-asteroid-bullet-collisions
  [state]
  (let [{:keys [bullets]} state]
    (reduce (fn [state bullet]
              (let [{:keys [asteroids]} state]
                (reduce (fn [state asteroid]
                          (let [{{asteroid-shape :shape} :space-object} asteroid
                                {{bullet-pos :pos} :space-object} bullet]
                            (if (so/shape-contains? asteroid-shape bullet-pos)
                              (reduced (-> state
                                           (merge {:asteroids (disj asteroids asteroid)
                                                   :bullets (disj bullets bullet)})
                                           (split-asteroid asteroid)))
                              state)))
                        state
                        asteroids)))
            state
            bullets)))

(defn- handle-player-asteroid-collisions
  [state]
  (let [{:keys [player asteroids]} state
        {{player-shape :shape} :space-object
         hit? :hit?} player]
    (if-not hit?
      (reduce (fn [state asteroid]
                (let [{{asteroid-shape :shape} :space-object} asteroid]
                  (if (so/shapes-intersect? player-shape asteroid-shape)
                    (-> state
                        (update :player #(p/player-hit %))
                        (update :asteroids #(disj % asteroid))
                        (split-asteroid asteroid))
                    state)))
              state
              asteroids)
      state)))

(defn- handle-collisions
  [state]
  (-> state
      (handle-asteroid-bullet-collisions)
      (handle-player-asteroid-collisions)))
