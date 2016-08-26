(ns clojuroids.player
  (:require [clojuroids.common :as c]
            [clojuroids.space-object :as so]
            [clojuroids.key-state :as ks])
  (:import [com.badlogic.gdx.math MathUtils]
           [com.badlogic.gdx Input$Keys]))

(def acceleration 100)
(def deceleration 10)
(def max-speed 300)
(def player-color [1 1 1 1])

(defrecord Player [space-object])

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
  (if (ks/key-down? Input$Keys/UP)
    (accelerate player delta-time)
    player))

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

(defn update-player!
  [player delta-time]
  (as-> player p
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
    (update p :space-object so/update-space-object! delta-time)))

(defn draw-player
  [player shape-renderer]
  (let [{:keys [space-object]} player]
    (so/draw-space-object space-object shape-renderer player-color)))

(defn make-player
  [& {:keys [pos]
      :or {pos [0 0]}}]
  (let [radians (/ Math/PI 2)]
    (map->Player {:space-object (so/make-space-object :pos pos
                                                      :radians radians
                                                      :shape (make-ship-shape pos radians))})))
