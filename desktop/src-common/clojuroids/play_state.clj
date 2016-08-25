(ns clojuroids.play-state
  (:require [clojure.set :as set]
            [clojuroids.common :as c]
            [clojuroids.game-state-manager :as gsm]
            [clojuroids.player :as p]
            [clojuroids.bullet :as b]
            [clojuroids.asteroids :as a]
            [clojuroids.particle :as part]
            [clojuroids.key-state :as ks]
            [clojuroids.space-object :as so]
            [clojuroids.jukebox :as j]
            [clojuroids.flying-saucer :as fs]
            [clojuroids.timer :as t])
  (:import [com.badlogic.gdx.graphics.glutils ShapeRenderer]
           [com.badlogic.gdx.graphics.g2d SpriteBatch]
           [com.badlogic.gdx.graphics Color]
           [com.badlogic.gdx Input$Keys]
           [com.badlogic.gdx.math MathUtils])
  (:gen-class))

(def min-delay 0.25)
(def max-delay 1)

(def max-bullets 4)

(defn- random-asteroid-location
  [player-pos]
  (let [[screen-width screen-height] c/screen-size
        [px py] player-pos
        [ax ay] [(MathUtils/random screen-width)
                 (MathUtils/random screen-height)]
        [dx dy] [(- ax px)
                 (- ay py)]
        dist (Math/sqrt (+ (* dx dx) (* dy dy)))]
    (if (< dist 100)
      (recur player-pos)
      [ax ay])))

(defn- spawn-asteroids
  [state]
  (let [{:keys [level player]} state
        player-pos (get-in player [:space-object :pos])
        num-to-spawn (+ 4 (- level 1))
        total-asteroids (* num-to-spawn 7)
        asteroids (for [_ (range num-to-spawn)]
                    (a/make-asteroid (random-asteroid-location player-pos)
                                     :large))]
    (merge state
           {:asteroids asteroids
            :total-asteroids total-asteroids
            :num-asteroids-left total-asteroids
            :current-delay max-delay})))

(defn shoot
  [state]
  (let [{:keys [player bullets]} state
        {:keys [space-object hit?]} player
        {:keys [pos radians]} space-object]
    (if (and (< (count bullets) max-bullets)
             (not hit?))
      (do
        (j/play-sound :shoot)
        (update state :bullets
                conj (b/make-bullet pos radians)))
      state)))

(declare handle-collisions handle-input)

(defn play-background-music
  [state delta-time]
  (-> state
      ((fn [{:keys [num-asteroids-left total-asteroids]
             :as state}]
         (assoc state :current-delay (+ (/ (* (- max-delay min-delay)
                                              num-asteroids-left)
                                           total-asteroids)
                                        min-delay))))
      (update :bg-timer + delta-time)
      ((fn [{{:keys [hit?]} :player
             :keys [bg-timer current-delay play-low-pulse?]
             :as state}]
         (if (and (not hit?) (>= bg-timer current-delay))
           (do (j/play-sound (if play-low-pulse?
                               :pulselow
                               :pulsehigh))
               (-> state
                   (update :play-low-pulse? not)
                   (assoc :bg-timer 0)))
           state)))))

(defn update-or-spawn-flying-saucer
  [state delta-time]
  (if (nil? (:flying-saucer state))
    (as-> state state
      (update state :fs-timer t/update-timer delta-time)
      (if (t/timer-elapsed? (:fs-timer state))
        (-> state
            (update :fs-timer t/reset-timer)
            (assoc :flying-saucer
                   (fs/make-flying-saucer (rand-nth [:left :right])
                                          (rand-nth [:large :small]))))
        state))
    (fs/update-flying-saucer state delta-time)))

(defn kill-flying-saucer
  [state]
  (-> state
      ((fn [s]
         (j/stop-sound :smallsaucer)
         (j/stop-sound :largesaucer)
         s))
      (assoc :flying-saucer nil)))

(defrecord PlayState [shape-renderer player
                      bullets asteroids particles
                      level total-asteroids num-asteroids-left
                      sprite-batch font
                      current-delay
                      bg-timer
                      play-low-pulse?
                      flying-saucer enemy-bullets]
  gsm/GameState

  (init [this]
    (let [[w h] c/screen-size]
      (-> this
          (merge {:shape-renderer  (ShapeRenderer.)
                  :sprite-batch    (SpriteBatch.)
                  :font            (c/gen-font 20 Color/WHITE)
                  :player          (p/make-player :pos [(/ w 2)(/ h 2)])
                  :level           1
                  :bullets         #{}
                  :asteroids       #{}
                  :particles       ()
                  :flying-saucer   nil
                  :enemy-bullets   #{}
                  :fs-timer        (t/make-timer 15)
                  ;; set up bg music
                  :current-delay   max-delay
                  :bg-timer        max-delay
                  :play-low-pulse? true})
          (spawn-asteroids))))

  (update! [this delta-time]
    (let [{:keys [dead? extra-lives score]} player]
      (cond
        ;; handle lose game
        (and dead? (zero? extra-lives))
        (do (gsm/set-state! :game-over score)
            (j/stop-all)
            this)

        ;; handle dead player
        dead?
        (-> this
            (update :player p/reset)
            (update :player p/lose-life)
            kill-flying-saucer)

        ;; new level when asteroids are gone
        (= 0 (count asteroids))
        (-> this
            (update :level + level)
            spawn-asteroids)

        ;; otherwise update all game objects
        :else (-> this
                  handle-input
                  (update :player p/update-player! delta-time)
                  (update :bullets b/update-bullets delta-time)
                  (update :enemy-bullets b/update-bullets delta-time)
                  (update-or-spawn-flying-saucer delta-time)
                  (update :asteroids a/update-asteroids delta-time)
                  (update :particles part/update-particles delta-time)
                  handle-collisions
                  (play-background-music delta-time)))))

  (draw [this]
    (.setProjectionMatrix sprite-batch (.combined @c/camera))
    (.setProjectionMatrix shape-renderer (.combined @c/camera))
    (p/draw-player player shape-renderer)
    (b/draw-bullets bullets shape-renderer)
    (b/draw-bullets enemy-bullets shape-renderer)
    (a/draw-asteroids asteroids shape-renderer)
    (part/draw-particles particles shape-renderer)
    (p/draw-score player sprite-batch font)
    (p/draw-player-lives player shape-renderer)
    (when-not (nil? flying-saucer)
      (fs/draw-flying-saucer flying-saucer shape-renderer)))

  (dispose [this]
    (.dispose sprite-batch)
    (.dispose shape-renderer)
    (.dispose font)))

(defn handle-input [this]
  (let [{:keys [player]} this]
    (if (ks/key-pressed? Input$Keys/SPACE)
        (shoot this)
        this)))

(defn make-play-state
  []
  (map->PlayState {}))

(defn- create-particles
  [state pos]
  (update state :particles
          (fn [particles]
            (into particles
                  (for [_ (range 6)]
                    (part/make-particle pos))))))

(defn- split-asteroid
  [state asteroid]
  (-> state
      (create-particles (-> asteroid :space-object :pos))
      (update :num-asteroids-left dec)
      (update :asteroids (fn [asteroids]
                           (let [{type :type
                                  {:keys [pos]} :space-object} asteroid]
                             (if (contains? #{:large :medium} type)
                               (let [new-type (case type
                                                :large :medium
                                                :medium :small)]
                                 (set/union asteroids
                                            (set [(a/make-asteroid pos new-type)
                                                  (a/make-asteroid pos new-type)])))
                               asteroids))))))

(defn find-collisions
  [coll-a coll-b fn-contains?]
  (reduce (fn [collisions a]
            (reduce (fn [collisions b]
                      (if (fn-contains? a b)
                        (reduced (conj collisions [a b]))
                        collisions))
                    collisions
                    coll-b))
          []
          coll-a))


(defn bullet-asteroid-collision?
  [{{bullet-pos :pos} :space-object}
   {{asteroid-shape :shape} :space-object}]
  (so/shape-contains? asteroid-shape bullet-pos))

(defn handle-bullet-asteroid-collision
  [{:keys [asteroids bullets] :as state} [bullet asteroid]]
  (j/play-sound :explode)
  (-> state
      (update :player p/increment-score (a/score asteroid))
      (update :asteroids disj asteroid)
      (update :bullets disj bullet)
      (split-asteroid asteroid)))

(defn- handle-bullet-asteroid-collisions
  [state]
  (reduce handle-bullet-asteroid-collision
          state
          (find-collisions (:bullets state)
                           (:asteroids state)
                           bullet-asteroid-collision?)))

(defn- handle-player-asteroid-collisions
  [{:keys [player asteroids] :as state}]
  (if-not (:hit? player)
    (reduce (fn [state [player asteroid]]
              (j/play-sound :explode)
              (-> state
                  (update :player p/player-hit)
                  (update :asteroids disj asteroid)
                  (split-asteroid asteroid)))
            state
            (find-collisions [(:player state)]
                             (:asteroids state)
                             (fn [player asteroid]
                               (so/shapes-intersect? (-> player :space-object :shape)
                                                     (-> asteroid :space-object :shape)))))
    state))

(defn handle-flying-saucer-player-collision
  [state]
  (let [{:keys [player flying-saucer]} state
        {{player-pos :pos
          player-shape :shape} :space-object} player
        {{fs-shape :shape
          fs-pos :pos} :space-object} flying-saucer]
    (if (and (not (nil? flying-saucer))
             (not (:hit? player))
             (not (:dead? player))
             (so/shapes-intersect? player-shape fs-shape))
      (do
        (j/play-sound :explode)
        (-> state
            (update :player p/player-hit)
            (create-particles player-pos)
            (create-particles fs-pos)
            kill-flying-saucer))
      state)))

(defn handle-enemy-bullets-asteroid-collisions
  [state]
  (let [{:keys [enemy-bullets asteroids]} state]
    (reduce (fn [state [enemy-bullet asteroid]]
              (j/play-sound :explode)
              (-> state
                  (update :asteroids disj asteroid)
                  (update :enemy-bullets disj enemy-bullet)
                  (split-asteroid asteroid)))
            state
            (find-collisions enemy-bullets
                             asteroids
                             (fn [enemy-bullet asteroid]
                               (so/shape-contains? (-> asteroid :space-object :shape)
                                                   (-> enemy-bullet :space-object :pos)))))))

(defn handle-enemy-bullets-player-collision
  [state]
  (let [{:keys [player enemy-bullets]} state
        {:keys [hit?]} player]
    (if-not hit?
      (reduce (fn [state [player bullet]]
                (j/play-sound :explode)
                (-> state
                    (update :player p/player-hit)
                    (create-particles (-> player :space-object :pos))
                    (update :enemy-bullets disj bullet)))
              state
              (find-collisions [player]
                               enemy-bullets
                               (fn [player bullet]
                                 (so/shape-contains? (-> player :space-object :shape)
                                                     (-> bullet :space-object :pos)))))
      state)))

(defn handle-flying-saucer-asteroid-collision
  [state]
  (let [{:keys [flying-saucer asteroids]} state]
    (if-not (nil? flying-saucer)
      (reduce (fn [state [flying-saucer asteroid]]
                (j/play-sound :explode)
                (-> state
                    (update :asteroids disj asteroid)
                    (split-asteroid asteroid)
                    (create-particles (-> flying-saucer :space-object :pos))
                    kill-flying-saucer))
              state
              (find-collisions [flying-saucer]
                               asteroids
                               (fn [flying-saucer asteroid]
                                 (so/shapes-intersect? (-> flying-saucer :space-object :shape)
                                                       (-> asteroid :space-object :shape)))))
      state)))

(defn handle-flying-saucer-player-bullet-collision
  [state]
  (let [{:keys [flying-saucer bullets]} state]
    (if-not (nil? flying-saucer)
      (reduce (fn [state [fs bullet]]
                (j/play-sound :explode)
                (-> state
                    (update :bullets disj bullet)
                    (create-particles (-> fs :space-object :pos))
                    (update :player p/increment-score (fs/score fs))
                    kill-flying-saucer))
              state
              (find-collisions [flying-saucer]
                               bullets
                               (fn [flying-saucer bullet]
                                 (so/shape-contains? (-> flying-saucer :space-object :shape)
                                                     (-> bullet :space-object :pos)))))
      state)))

(defn- handle-collisions
  [state]
  (-> state
      handle-bullet-asteroid-collisions
      handle-player-asteroid-collisions
      handle-enemy-bullets-player-collision
      handle-enemy-bullets-asteroid-collisions
      handle-flying-saucer-player-collision
      handle-flying-saucer-asteroid-collision
      handle-flying-saucer-player-bullet-collision))
