(ns clojuroids.player
  (:require [clojuroids.common :as c]
            [clojuroids.space-object :as so]
            [clojuroids.jukebox :as j]
            [clojuroids.key-state :as ks]
            [clojuroids.timer :as t])
  (:import [com.badlogic.gdx.math MathUtils]
           [com.badlogic.gdx.graphics.g2d Batch]
           [com.badlogic.gdx Input$Keys]))

(def acceleration 100)
(def deceleration 10)
(def max-speed 300)
(def player-color [1 1 1 1])
(def extra-life-increment 10000)

(defrecord Player [space-object
                   accelerating-timer flame
                   hit? dead?
                   hit-lines hit-lines-vector
                   hit-timer
                   score extra-lives required-score])

(defn- make-ship-shape
  [pos radians]
  (map (partial so/vector-add pos)
       [(so/make-vector radians 8)
        (so/make-vector (- radians (/ (* 4 Math/PI) 5)) 8)
        (so/make-vector (+ radians Math/PI) 5)
        (so/make-vector (+ radians (/ (* 4 Math/PI) 5)) 8)]))

(defn- update-ship-shape
  [player]
  (let [{:keys [space-object]} player
        {:keys [pos radians]} space-object]
    (assoc-in player [:space-object :shape]
              (make-ship-shape pos radians))))

(defn- make-flame-shape
  [pos radians accelerating-timer]
  (map (partial so/vector-add pos)
       [(so/make-vector (- radians (/ (* 5 Math/PI) 6)) 5)
        (so/make-vector (- radians Math/PI) (+ 6 (* (t/timer-value accelerating-timer) 50)))
        (so/make-vector (+ radians (/ (* 5 Math/PI) 6)) 5)]))

(defn- accelerate
  [player delta-time]
  (let [{:keys [space-object]} player
        {:keys [dpos radians]} space-object]
    (assoc-in player [:space-object :dpos]
              (-> (so/make-vector radians
                                  (* acceleration delta-time))
                  (so/vector-add dpos)))))

(defn- decelerate
  [player delta-time]
  (let [{:keys [space-object]} player
        {[dx dy] :dpos} space-object
        vec (Math/sqrt (+ (* dx dx) (* dy dy)))]
    (assoc-in player [:space-object :dpos]
              (cond (> vec max-speed) [(* (/ dx vec) max-speed)
                                       (* (/ dy vec) max-speed)]

                    (> vec 0) [(- dx (* (/ dx vec) deceleration delta-time))
                               (- dy (* (/ dy vec) deceleration delta-time))]

                    :else [dx dy]))))

(defn- update-acceleration
  [player delta-time]
  (let [{:keys [hit?]} player]
    (if (and (not hit?)
             (ks/key-down? Input$Keys/UP))
      (do (j/loop-sound :thruster)
          (as-> player p
            (let [{:keys [space-object accelerating-timer]} p
                  {:keys [pos radians]} space-object]
              (assoc p :flame (make-flame-shape pos radians accelerating-timer)))
            (accelerate p delta-time)
            (update p :accelerating-timer #(let [at (t/update-timer % delta-time)]
                                             (if (t/timer-elapsed? at)
                                               (t/reset-timer at)
                                               at)))))
      (do (j/stop-sound :thruster)
          (assoc player :flame [])))))

(defn- turn
  [player delta-time sign]
  (let [{{:keys [rotation-speed]} :space-object} player]
    (update-in player [:space-object :radians]
               (fn [radians]
                 (sign radians (* rotation-speed delta-time))))))

(defn- turn-left
  [player delta-time]
  (turn player delta-time +))

(defn- turn-right
  [player delta-time]
  (turn player delta-time -))

(defn- update-extra-lives
  [{:keys [score required-score]
    :as player}]
  (if (>= score required-score)
    (do (j/play-sound :extralife)
        (-> player
            (update :extra-lives inc)
            (update :required-score + extra-life-increment)))
    player))

(defn- update-dead-player
  [player delta-time]
  (j/stop-sound :thruster)
  (-> player
      ;; handle expiring hit timer
      (update :hit-timer t/update-timer delta-time)
      ((fn [player]
         (let [{:keys [hit-timer]} player]
           (merge player
                  (if (t/timer-elapsed? hit-timer)
                      {:hit-timer (t/reset-timer hit-timer)
                       :dead? true}
                      {:hit-timer hit-timer})))))
      ;; player explode animation
      ((fn [player]
         (let [{:keys [hit-lines-vector]} player]
           (update player :hit-lines (fn [hit-lines]
                                       (map (fn [[[x1 y1] [x2 y2]] [vx vy]]
                                              [[(+ x1 (* vx 10 delta-time))
                                                (+ y1 (* vy 10 delta-time))]
                                               [(+ x2 (* vx 10 delta-time))
                                                (+ y2 (* vy 10 delta-time))]])
                                            hit-lines
                                            hit-lines-vector))))))))

(defn update-player!
  [player delta-time]
  (let [{:keys [hit? space-object]} player]
    (if hit?
      ;; update dead player
      (update-dead-player player delta-time)
      ;; update alive player
      (as-> player p
        ;; check extra lives
        (update-extra-lives p)
        ;; turning
        (cond (ks/key-down? Input$Keys/LEFT) (turn-left p delta-time)
              (ks/key-down? Input$Keys/RIGHT)(turn-right p delta-time)
              :else p)
        ;; acceleration
        (update-acceleration p delta-time)
        ;; deceleration
        (decelerate p delta-time)
        ;; update shapes
        (update-ship-shape p)
        ;; update space-object
        (update p :space-object so/update-space-object! delta-time)))))

(defn draw-player
  [player shape-renderer]
  (let [{:keys [space-object flame hit? hit-lines]} player]
    (if hit?
      (so/draw-lines shape-renderer hit-lines player-color)
      (do (so/draw-space-object space-object shape-renderer player-color)
          (so/draw-shape shape-renderer flame player-color)))))

(defn make-player
  [& {:keys [pos]
      :or {pos [0 0]}}]
  (let [radians (/ Math/PI 2)]
    (map->Player {:space-object (so/make-space-object :pos pos
                                                      :radians radians
                                                      :shape (make-ship-shape pos radians))
                  :flame []
                  :accelerating-timer (t/make-timer 0.1)
                  :hit? false
                  :hit-timer (t/make-timer 2)
                  :score 0
                  :extra-lives 3
                  :required-score extra-life-increment})))

(defn player-hit
  [player]
  (let [{hit? :hit?
         {:keys [shape radians]} :space-object} player]
    (if-not hit?
      (as-> player player
        (merge player {:hit? true
                       :hit-lines (so/vert-lines-seq shape)
                       :hit-lines-vector [(so/make-vector (+ radians 1.5) 1)
                                          (so/make-vector (- radians 1.5) 1)
                                          (so/make-vector (- radians 2.8) 1)
                                          (so/make-vector (+ radians 2.8) 1)]})
        (assoc-in player [:space-object :dpos] [0 0]))
      player)))

(defn reset
  [player]
  (let [[w h] c/screen-size]
    (as-> player p
      (assoc-in p [:space-object :pos] [(/ w 2) (/ h 2)])
      (update-ship-shape p)
      (merge p
             {:hit? false
              :dead? false}))))

(defn lose-life
  [player]
  (update player :extra-lives dec))

(defn increment-score
  [player n]
  (update player :score + n))

(defn draw-score
  [player sprite-batch font]
  (let [{score :score} player]
    (.setColor sprite-batch 1 1 1 1)
    (.begin sprite-batch)
    (.draw font sprite-batch (str score) (float 40) (float 390))
    (.end sprite-batch)))

(defn draw-player-lives
  [player shape-renderer]
  (let [{:keys [extra-lives]} player]
    (dotimes [i extra-lives]
      (let [player (make-player :pos [(+ 40 (* i 10)) 360])]
        (draw-player player shape-renderer)))))
