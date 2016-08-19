(ns clojuroids.bullet
  (:require [clojuroids.space-object :as so])
  (:import [com.badlogic.gdx.math MathUtils]))

(defrecord Bullet
  [space-object life-time life-timer remove?])

(defn make-bullet
  [[x y] radians]
  (let [speed 350]
    (map->Bullet {:space-object (so/map->SpaceObject {:pos     [x y]
                                                      :radians radians
                                                      :dpos    [(* (MathUtils/cos radians) speed)
                                                                (* (MathUtils/sin radians) speed)]
                                                      :size    [2 2]})
                  :remove?      false
                  :life-timer   0
                  :life-time    1})))

(defn make-bullet-shape
  [{{radians :radians
     [x y]   :pos   } :space-object}]
  [[(+ x 1)
      (+ y 1)]
     [(+ x 1)
      (- y 1)]
     [(- x 1)
      (- y 1)]])

(defn update-bullet
  [bullet delta-time]
  (as-> bullet b
        (update b :space-object #(-> %
                                     (so/update-space-object! delta-time)
                                     (assoc :shape (make-bullet-shape b))))
        (update b :life-timer #(+ % delta-time))
        (assoc b :remove? (let [{:keys [life-timer life-time]} b]
                            (> life-timer life-time)))))

(defn update-bullets
  [bullets delta-time]
  (->> bullets
       (map #(update-bullet % delta-time))
       (remove :remove?)
       (into #{})))

(def bullet-color [1 1 1 1])

(defn draw-bullet
  [bullet shape-renderer]
  (let [{:keys [space-object]} bullet]
    (so/draw-space-object space-object shape-renderer bullet-color)))

(defn draw-bullets
  [bullets shape-renderer]
  (doseq [bullet bullets]
    (draw-bullet bullet shape-renderer)))
