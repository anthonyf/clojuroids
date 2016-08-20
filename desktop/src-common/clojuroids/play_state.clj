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
            [clojuroids.jukebox :as j])
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
                #(conj %
                       (b/make-bullet pos radians))))
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
      (update :bg-timer #(+ % delta-time))
      ((fn [{{:keys [hit?]} :player
             :keys [bg-timer current-delay play-low-pulse?]
             :as state}]
         (if (and (not hit?) (>= bg-timer current-delay))
           (do (j/play-sound (if play-low-pulse?
                               :pulselow
                               :pulsehigh))
               (-> state
                   (update :play-low-pulse? #(not %))
                   (assoc :bg-timer 0)))
           state)))))

(defrecord PlayState [shape-renderer player
                      bullets asteroids particles
                      level total-asteroids num-asteroids-left
                      sprite-batch font
                      current-delay
                      bg-timer
                      play-low-pulse?]
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
                  ;; set up bg music
                  :current-delay   max-delay
                  :bg-timer        max-delay
                  :play-low-pulse? true})
          (spawn-asteroids))))

  (update! [this delta-time]
    (let [{:keys [dead? extra-lives]} player]
      (cond
        ;; handle lose game
        (and dead? (zero? extra-lives))
        (do (gsm/set-state! :menu)
            this)

        ;; handle dead player
        dead?
        (-> this
            (update :player #(p/reset %))
            (update :player p/lose-life))

        ;; new level when asteroids are gone
        (= 0 (count asteroids))
        (-> this
            (update :level #(+ level %))
            (spawn-asteroids))

        ;; otherwise update all game objects
        :else (-> this
                  (handle-input)
                  (update :player #(p/update-player! % delta-time))
                  (update :bullets #(b/update-bullets % delta-time))
                  (update :asteroids #(a/update-asteroids % delta-time))
                  (update :particles #(part/update-particles % delta-time))
                  (handle-collisions)
                  (play-background-music delta-time)))))

  (draw [this]
    (.setProjectionMatrix sprite-batch (.combined @c/camera))
    (.setProjectionMatrix shape-renderer (.combined @c/camera))
    (p/draw-player player shape-renderer)
    (b/draw-bullets bullets shape-renderer)
    (a/draw-asteroids asteroids shape-renderer)
    (part/draw-particles particles shape-renderer)
    (p/draw-score player sprite-batch font)
    (p/draw-player-lives player shape-renderer))

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

(defn- handle-asteroid-bullet-collisions
  [{:keys [bullets] :as state}]
  (reduce (fn [{:keys [asteroids] :as state}
               {{bullet-pos :pos} :space-object :as bullet}]
            (reduce (fn [state {{asteroid-shape :shape} :space-object :as asteroid}]
                      (if (so/shape-contains? asteroid-shape bullet-pos)
                        (do (j/play-sound :explode)
                            (reduced (-> state
                                         (update :player #(p/increment-score % (a/score asteroid)))
                                         (merge {:asteroids (disj asteroids asteroid)
                                                 :bullets (disj bullets bullet)})
                                         (split-asteroid asteroid))))
                        state))
                    state
                    asteroids))
          state
          bullets))

(defn- handle-player-asteroid-collisions
  [{:keys [player asteroids] :as state}]
  (let [{{player-shape :shape} :space-object
         hit?                  :hit?} player]
    (if-not hit?
      (reduce (fn [state
                   {{asteroid-shape :shape} :space-object :as asteroid}]
                (if (so/shapes-intersect? player-shape asteroid-shape)
                  (do (j/play-sound :explode)
                      (-> state
                          (update :player #(p/player-hit %))
                          (update :asteroids #(disj % asteroid))
                          (split-asteroid asteroid)))
                  state))
              state
              asteroids)
      state)))

(defn- handle-collisions
  [state]
  (-> state
      (handle-asteroid-bullet-collisions)
      (handle-player-asteroid-collisions)))
