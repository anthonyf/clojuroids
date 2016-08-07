(ns clojuroids.player
  (:require [clojuroids.space-object :refer :all])
  (:import [com.badlogic.gdx.math MathUtils]
           [com.badlogic.gdx.graphics.g2d Batch]))

(defrecord Player [space-object
                   left? right? up? max-speed acceleration deceleration
                   accelerating-timer flame
                   hit? dead?
                   hit-lines hit-lines-vector
                   hit-timer hit-time
                   score extra-lives required-score])

(defn- make-ship-shape
  [player]
  (let [{:keys [space-object]} player
        {:keys [pos radians]} space-object
        [x y] pos]
    [[(+ x (* (MathUtils/cos radians) 8))
      (+ y (* (MathUtils/sin radians) 8))]
     [(+ x (* (MathUtils/cos (- radians (/ (* 4 Math/PI) 5))) 8))
      (+ y (* (MathUtils/sin (- radians (/ (* 4 Math/PI) 5))) 8))]
     [(+ x (* (MathUtils/cos (+ radians Math/PI)) 5))
      (+ y (* (MathUtils/sin (+ radians Math/PI)) 5))]
     [(+ x (* (MathUtils/cos (+ radians (/ (* 4 Math/PI) 5))) 8))
      (+ y (* (MathUtils/sin (+ radians (/ (* 4 Math/PI) 5))) 8))]]))


(defn- make-flame-shape
  [player]
  (let [{:keys [space-object accelerating-timer]} player
        {:keys [pos radians]} space-object
        [x y] pos]
    [[(+ x (* (MathUtils/cos (- radians (/ (* 5 Math/PI) 6))) 5))
      (+ y (* (MathUtils/sin (- radians (/ (* 5 Math/PI) 6))) 5))]
     [(+ x (* (MathUtils/cos (- radians Math/PI))
              (+ 6 (* accelerating-timer 50))))
      (+ y (* (MathUtils/sin (- radians Math/PI))
              (+ 6 (* accelerating-timer 50))))]
     [(+ x (* (MathUtils/cos (+ radians (/ (* 5 Math/PI) 6))) 5))
      (+ y (* (MathUtils/sin (+ radians (/ (* 5 Math/PI) 6))) 5))]]))


(defn- accelerate
  [player delta-time]
  (let [{:keys [space-object acceleration]} player
        {[dx dy] :dpos
         :keys [radians]} space-object]
    (assoc-in player [:space-object :dpos]
              [(+ dx (* (MathUtils/cos radians) acceleration delta-time))
               (+ dy (* (MathUtils/sin radians) acceleration delta-time))])))

(defn- decelerate
  [player delta-time]
  (let [{:keys [space-object
                deceleration
                max-speed]} player
        {[dx dy] :dpos} space-object
        vec (Math/sqrt (+ (* dx dx) (* dy dy)))]
    (assoc-in player [:space-object :dpos]
              (cond (> vec 0) [(- dx (* (/ dx vec) deceleration delta-time))
                               (- dy (* (/ dy vec) deceleration delta-time))]
                    (> vec max-speed) [(* (/ dx vec) max-speed)
                                       (* (/ dy vec) max-speed)]
                    :else [dx dy]))))

(defn- turn
  [player delta-time sign]
  (let [{{:keys [radians rotation-speed]} :space-object} player]
    (assoc-in player [:space-object :radians]
              (sign radians (* rotation-speed delta-time)))))

(defn- turn-left
  [player delta-time]
  (turn player delta-time +))

(defn- turn-right
  [player delta-time]
  (turn player delta-time -))

(defn update-player!
  [player screen-size delta-time]
  (let [{:keys [left? right? up? accelerating-timer
                hit? hit-timer hit-time hit-lines
                hit-lines-vector score required-score]} player]
    (if hit?
      ;; update dead player
      (-> player
          (merge (let [hit-timer (+ hit-timer delta-time)]
                   (if (> hit-timer hit-time)
                     {:hit-timer 0
                      :dead? true}
                     {:hit-timer hit-timer})))
          (update :hit-lines (fn [hit-lines]
                               (map (fn [[[x1 y1] [x2 y2]] [vx vy]]
                                      [[(+ x1 (* vx 10 delta-time))
                                        (+ y1 (* vy 10 delta-time))]
                                       [(+ x2 (* vx 10 delta-time))
                                        (+ y2 (* vy 10 delta-time))]])
                                    hit-lines
                                    hit-lines-vector))))
      ;; update alive player
      (as-> player p
        ;; check extra lives
        (if (>= score required-score)
          (-> p
              (update :extra-lives inc)
              (update :required-score #(+ % 10000)))
          p)
        ;; turning
        (cond left? (turn-left p delta-time)
              right? (turn-right p delta-time)
              :else p)
        ;; acceleration
        (if up?
          (-> p
              (accelerate delta-time)
              (assoc :accelerating-timer
                     (as-> accelerating-timer at
                       (+ at delta-time)
                       (if (> at 0.1)
                         0
                         at))))
          p)
        ;; deceleration
        (decelerate p  delta-time)
        ;; update shapes
        (assoc-in p [:space-object :shape] (make-ship-shape p))
        (assoc p :flame (if up?
                          (make-flame-shape p)
                          []))
        ;; update space-object
        (assoc p :space-object (update-space-object! (:space-object p) screen-size delta-time))))))

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
  [[screen-width screen-height]]
  (let [radians (/ Math/PI 2)
        pos [(/ screen-width 2)
             (/ screen-height 2)]]
    (map->Player {:space-object (make-space-object pos radians)
                  :left? false
                  :right? false
                  :up? false
                  :acceleration 200
                  :deceleration 10
                  :max-speed 300
                  :flame []
                  :accelerating-timer 0
                  :hit? false
                  :hit-timer 0
                  :hit-time 2
                  :score 0
                  :extra-lives 3
                  :required-score 10000})))

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
  [player [screen-width screen-height]]
  (as-> player p
    (assoc-in p [:space-object :pos] [(/ screen-width 2) (/ screen-height 2)])
    (assoc-in p [:space-object :shape] (make-ship-shape p))
    (merge p {:hit? false
              :dead? false})))

(defn lose-life
  [player]
  (update player :extra-lives dec))

(defn increment-score
  [player n]
  (update player :score #(+ % n)))

(defn draw-score
  [player sprite-batch font]
  (let [[r g b a] score-color
        {score :score} player]
    (.setColor sprite-batch 1 1 1 1)
    (.begin sprite-batch)
    (.draw font sprite-batch (str score) (float 40) (float 390))
    (.end sprite-batch)))
