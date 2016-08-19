(ns clojuroids.player
  (:require [clojuroids.common :as c]
            [clojuroids.space-object :refer :all]
            [clojuroids.jukebox :as j])
  (:import [com.badlogic.gdx.math MathUtils]
           [com.badlogic.gdx.graphics.g2d Batch]))

(def acceleration 100)
(def deceleration 10)
(def max-speed 300)

(defrecord Player [space-object
                   left? right? up?
                   accelerating-timer flame
                   hit? dead?
                   hit-lines hit-lines-vector
                   hit-timer hit-time
                   score extra-lives required-score])

(defn- make-ship-shape
  [[x y] radians]
  [[(+ x (* (MathUtils/cos radians) 8))
    (+ y (* (MathUtils/sin radians) 8))]
   [(+ x (* (MathUtils/cos (- radians (/ (* 4 Math/PI) 5))) 8))
    (+ y (* (MathUtils/sin (- radians (/ (* 4 Math/PI) 5))) 8))]
   [(+ x (* (MathUtils/cos (+ radians Math/PI)) 5))
    (+ y (* (MathUtils/sin (+ radians Math/PI)) 5))]
   [(+ x (* (MathUtils/cos (+ radians (/ (* 4 Math/PI) 5))) 8))
    (+ y (* (MathUtils/sin (+ radians (/ (* 4 Math/PI) 5))) 8))]])

(defn- update-ship-shape
  [player]
  (let [{:keys [space-object]} player
        {:keys [pos radians]} space-object]
    (assoc-in player [:space-object :shape]
              (make-ship-shape pos radians))))

(defn- make-flame-shape
  [[x y] radians accelerating-timer]
  [[(+ x (* (MathUtils/cos (- radians (/ (* 5 Math/PI) 6))) 5))
    (+ y (* (MathUtils/sin (- radians (/ (* 5 Math/PI) 6))) 5))]
   [(+ x (* (MathUtils/cos (- radians Math/PI))
            (+ 6 (* accelerating-timer 50))))
    (+ y (* (MathUtils/sin (- radians Math/PI))
            (+ 6 (* accelerating-timer 50))))]
   [(+ x (* (MathUtils/cos (+ radians (/ (* 5 Math/PI) 6))) 5))
    (+ y (* (MathUtils/sin (+ radians (/ (* 5 Math/PI) 6))) 5))]])

(defn- accelerate
  [player delta-time]
  (let [{:keys [space-object]} player
        {[dx dy] :dpos
         :keys [radians]} space-object]
    (assoc-in player [:space-object :dpos]
              [(+ dx (* (MathUtils/cos radians) acceleration delta-time))
               (+ dy (* (MathUtils/sin radians) acceleration delta-time))])))

(defn- decelerate
  [player delta-time]
  (let [{:keys [space-object]} player
        {[dx dy] :dpos} space-object
        vec (Math/sqrt (+ (* dx dx) (* dy dy)))]
    (assoc-in player [:space-object :dpos]
              (cond (> vec 0) [(- dx (* (/ dx vec) deceleration delta-time))
                               (- dy (* (/ dy vec) deceleration delta-time))]
                    (> vec max-speed) [(* (/ dx vec) max-speed)
                                       (* (/ dy vec) max-speed)]
                    :else [dx dy]))))

(defn- update-acceleration
  [player delta-time]
  (let [{:keys [up?]} player]
    (if up?
      (as-> player p
        (let [{:keys [space-object accelerating-timer]} p
              {[dx dy] :dpos
               :keys [pos radians]} space-object]
          (assoc p :flame (make-flame-shape pos radians accelerating-timer)))
        (accelerate p delta-time)
        (update p :accelerating-timer
                #(let [at (+ % delta-time)]
                    (if (> at 0.1)
                      0
                      at))))
      player)))

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

(def extra-life-increment 10000)

(defn- update-extra-lives
  [{:keys [score required-score]
    :as player}]
  (if (>= score required-score)
      (do (j/play-sound :extralife)
          (-> player
              (update :extra-lives inc)
              (update :required-score #(+ % extra-life-increment))))
      player))

(defn- update-dead-player
  [player delta-time]
  (-> player
      ;; handle expiring hit timer
      ((fn [player]
         (let [{:keys [hit-timer hit-time]} player]
           (merge player
                  (let [hit-timer (+ hit-timer delta-time)]
                    (if (> hit-timer hit-time)
                      {:hit-timer 0
                       :dead? true}
                      {:hit-timer hit-timer}))))))
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
        (let [{:keys [left? right?]} p]
          (cond left? (turn-left p delta-time)
                     right? (turn-right p delta-time)
                     :else p))
        ;; acceleration
        (update-acceleration p delta-time)
        ;; deceleration
        (decelerate p delta-time)
        ;; update shapes
        (update-ship-shape p)
        ;; update space-object
        (update p :space-object #(update-space-object! % delta-time))))))

(def player-color [1 1 1 1])

(defn draw-player
  [player shape-renderer]
  (let [{:keys [space-object up? flame hit? hit-lines]} player]
    (if hit?
      (draw-lines shape-renderer hit-lines player-color)
      (do (draw-space-object space-object shape-renderer player-color)
          (when up?
            (draw-shape shape-renderer flame player-color))))))

(defn make-player
  [& {:keys [pos]
      :or {pos [0 0]}}]
  (let [radians (/ Math/PI 2)]
    (map->Player {:space-object (make-space-object :pos pos
                                                   :radians radians
                                                   :shape (make-ship-shape pos radians))
                  :left? false
                  :right? false
                  :up? false
                  :flame []
                  :accelerating-timer 0
                  :hit? false
                  :hit-timer 0
                  :hit-time 2
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
                       :left? false
                       :right? false
                       :up? false
                       :hit-lines (vert-lines-seq shape)
                       :hit-lines-vector [[(MathUtils/cos (+ radians 1.5))
                                           (MathUtils/sin (+ radians 1.5))]
                                          [(MathUtils/cos (- radians 1.5))
                                           (MathUtils/sin (- radians 1.5))]
                                          [(MathUtils/cos (- radians 2.8))
                                           (MathUtils/sin (- radians 2.8))]
                                          [(MathUtils/cos (+ radians 2.8))
                                           (MathUtils/sin (+ radians 2.8))]]})
        (update player :space-object (fn [so] (assoc so :dpos [0 0]))))
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
  (update player :score #(+ % n)))

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
