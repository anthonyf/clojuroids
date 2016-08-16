(ns clojuroids.play-state
  (:require [clojure.set :as set]
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
           [com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
            FreeTypeFontGenerator$FreeTypeFontParameter]
           [com.badlogic.gdx Gdx]
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
  (let [{:keys [level player screen-size-ref max-delay]} state
        player-pos (get-in player [:space-object :pos])
        num-to-spawn (+ 4 (- level 1))
        total-asteroids (* num-to-spawn 7)
        asteroids (for [_ (range num-to-spawn)]
                    (a/make-asteroid (random-asteroid-location @screen-size-ref player-pos)
                                     :large))]
    (merge state
           {:asteroids asteroids
            :total-asteroids total-asteroids
            :num-asteroids-left total-asteroids
            :current-delay max-delay})))

(def ^:const max-bullets 4)

(defn shoot
  [state]
  (j/play-sound :shoot)
  (let [{:keys [player bullets]} state
        {:keys [space-object]} player
        {:keys [pos radians]} space-object]
    (if (< (count bullets) max-bullets)
      (update state :bullets #(conj %
                                    (b/make-bullet pos radians)))
      state)))

(declare handle-collisions)

(defn play-background-music
  [state delta-time]
  (-> state
      (update :bg-timer #(+ % delta-time))
      ((fn [state]
         (let [{:keys [player bg-timer current-delay play-low-pulse?]} state
               {:keys [hit?]} player]
           (if (and (not hit?) (>= bg-timer current-delay))
             (do (j/play-sound (if play-low-pulse?
                                 :pulselow
                                 :pulsehigh))
                 (-> state
                     (update :play-low-pulse? #(not %))
                     (assoc :bg-timer 0)))
             state))))))

(defrecord PlayState [screen-size-ref shape-renderer player
                      bullets asteroids particles
                      level total-asteroids num-asteroids-left
                      sprite-batch font
                      max-delay min-delay current-delay
                      bg-timer
                      play-low-pulse?]
  gsm/game-state
  (init [this]
    (let [[w h] @screen-size-ref
          max-delay 1]
      (-> this
          (merge {:shape-renderer  (ShapeRenderer.)
                  :sprite-batch    (SpriteBatch.)
                  :font            (let [gen (FreeTypeFontGenerator. (.internal Gdx/files "fonts/Hyperspace Bold.ttf"))]
                                     (.generateFont gen (doto (FreeTypeFontGenerator$FreeTypeFontParameter.)
                                                          (-> .size (set! 20)))))
                  :player          (p/make-player :pos [(/ w 2)(/ h 2)])
                  :level           1
                  :bullets         #{}
                  :asteroids       #{}
                  :particles       ()
                  ;; set up bg music
                  :max-delay       max-delay
                  :min-delay       0.25
                  :current-deley   max-delay
                  :bg-timer        max-delay
                  :play-low-pulse? true})
          (spawn-asteroids))))

  (update! [this screen-size delta-time]
    (let [{dead? :dead?} player]
      (cond dead?
            (-> this
                (update :player #(p/reset % screen-size))
                (update :player p/lose-life))

            (= 0 (count asteroids))
            (-> this
                (update :level #(+ level %))
                (spawn-asteroids))

            :else (-> this
                      (update :player #(p/update-player! % screen-size delta-time))
                      (update :bullets #(b/update-bullets % screen-size delta-time))
                      (update :asteroids #(a/update-asteroids % screen-size delta-time))
                      (update :particles #(part/update-particles % screen-size delta-time))
                      (handle-collisions)
                      (play-background-music delta-time)))))

  (draw [this]
    (p/draw-player player shape-renderer)
    (b/draw-bullets bullets shape-renderer)
    (a/draw-asteroids asteroids shape-renderer)
    (part/draw-particles particles shape-renderer)
    (p/draw-score player sprite-batch font)
    (p/draw-player-lives player shape-renderer))

  (handle-input [this]
    (as-> this state
      (assoc state :player (-> player
                               (assoc :left? (ks/key-down? Input$Keys/LEFT))
                               (assoc :right? (ks/key-down? Input$Keys/RIGHT))
                               ((fn [player]
                                  (let [{:keys [up? hit?]} player
                                        key-up? (and (ks/key-down? Input$Keys/UP)
                                                     (not hit?))]
                                    (cond (and key-up?
                                               (not up?)) (j/loop-sound :thruster)
                                          (not key-up?)   (j/stop-sound :thruster))
                                    (assoc player :up? key-up?))))))
      (if (ks/key-pressed? Input$Keys/SPACE)
        (shoot state)
        state)))

  (dispose [this]))

(defn make-play-state
  [screen-size-ref]
  (map->PlayState {:screen-size-ref screen-size-ref}))

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
      ((fn [state]
         (let [{:keys [max-delay min-delay num-asteroids-left total-asteroids]} state]
           (assoc state :current-delay (+ (/ (* (- max-delay min-delay)
                                                num-asteroids-left)
                                             total-asteroids)
                                          min-delay)))))
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
                              (do (j/play-sound :explode)
                                  (reduced (-> state
                                               (update :player #(p/increment-score % (a/score asteroid)))
                                               (merge {:asteroids (disj asteroids asteroid)
                                                       :bullets (disj bullets bullet)})
                                               (split-asteroid asteroid))))
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
                    (do (j/play-sound :explode)
                        (-> state
                         (update :player #(p/player-hit %))
                         (update :asteroids #(disj % asteroid))
                         (split-asteroid asteroid)))
                    state)))
              state
              asteroids)
      state)))

(defn- handle-collisions
  [state]
  (-> state
      (handle-asteroid-bullet-collisions)
      (handle-player-asteroid-collisions)))
