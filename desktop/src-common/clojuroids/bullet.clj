(ns clojuroids.bullet
  (:require [clojuroids.space-object :as so]
            [clojuroids.timer :as t])
  (:import [com.badlogic.gdx.math MathUtils]))

(defrecord Bullet
    [space-object life-timer remove?])

(defn- make-bullet-shape
  [[x y]]
  [[(+ x 1)
    (+ y 1)]
   [(+ x 1)
    (- y 1)]
   [(- x 1)
    (- y 1)]])

(defn make-bullet
  [pos radians]
  (let [speed 350]
    (map->Bullet {:space-object (so/map->SpaceObject {:pos     pos
                                                      :radians radians
                                                      :dpos    (so/make-vector radians
                                                                               speed)
                                                      :size    [2 2]
                                                      :shape   (make-bullet-shape pos)})
                  :remove?      false
                  :life-timer   (t/make-timer 1)})))

(defn update-bullet
  [bullet delta-time]
  (as-> bullet b
    (update b :space-object #(-> %
                                 (so/update-space-object! delta-time)
                                 (assoc :shape (make-bullet-shape (-> b :space-object :pos)))))
    (update b :life-timer t/update-timer delta-time)
    (assoc b :remove? (let [{:keys [life-timer]} b]
                        (t/timer-elapsed? life-timer)))))

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
