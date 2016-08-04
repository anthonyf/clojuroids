(ns clojuroids.player
  (:require [clojuroids.space-object :refer :all])
  (:import [com.badlogic.gdx.math MathUtils]))

(defrecord Player [space-object
                   left? right? up? max-speed acceleration deceleration
                   accelerating-timer flame])

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
  (let [{:keys [left? right? up? accelerating-timer]} player]
    (as-> player p
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
      (assoc p :space-object (update-space-object! (:space-object p) screen-size delta-time)))))

(def player-color [1 1 1 1])

(defn draw-player
  [player shape-renderer]
  (let [{:keys [space-object up? flame]} player]
    (draw-space-object space-object shape-renderer player-color)
    (when up?
      (draw-shape shape-renderer flame player-color))))

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
                  :accelerating-timer 0})))

(defn player-hit
  [player]
  (println "player-hit!")
  player)
