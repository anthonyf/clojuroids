(ns clojuroids.bullet
  (:require [clojuroids.space-object :as so]
            [clojuroids.timer :as t])
  (:import [com.badlogic.gdx.math MathUtils]
           [com.badlogic.gdx.graphics.glutils
            ShapeRenderer ShapeRenderer$ShapeType]))

(defrecord Bullet
    [space-object life-timer remove?])

(defn make-bullet
  [pos radians]
  (let [speed 350]
    (map->Bullet {:space-object (so/map->SpaceObject {:pos     pos
                                                      :radians radians
                                                      :dpos    (so/make-vector radians
                                                                               speed)
                                                      :size    [2 2]})
                  :remove?      false
                  :life-timer   (t/make-timer 1)})))

(defn update-bullet
  [bullet delta-time]
  (as-> bullet b
    (update b :space-object so/update-space-object! delta-time)
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
  (let [[r g b a] bullet-color
        {:keys [space-object]} bullet
        {[width height] :size
         [x y]          :pos} space-object]
    (.setColor shape-renderer r g b a)
    (.begin shape-renderer ShapeRenderer$ShapeType/Line)
    (.circle shape-renderer (- x (/ width 2)) (- y (/ height 2)) (/ width 2))
    (.end shape-renderer)))

(defn draw-bullets
  [bullets shape-renderer]
  (doseq [bullet bullets]
    (draw-bullet bullet shape-renderer)))
