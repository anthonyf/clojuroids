(ns clojuroids.particle
  (:require [clojuroids.space-object :as so])
  (:import [com.badlogic.gdx.math MathUtils]
           [com.badlogic.gdx.graphics.glutils
            ShapeRenderer ShapeRenderer$ShapeType]))


(defrecord Particle
    [space-object timer time remove?])

(defn make-particle
  [pos]
  (let [speed 50
        radians (MathUtils/random (* 2 Math/PI))]
    (map->Particle
     {:space-object (so/map->SpaceObject {:radians radians
                                          :size [2 2]
                                          :pos  pos
                                          :dpos [(* speed (MathUtils/cos radians))
                                                 (* speed (MathUtils/sin radians))]})
      :timer 0
      :time 1
      :remove? false})))

(defn update-particle
  [particle screen-size delta-time]
  (as-> particle p
    (update p :space-object #(so/update-space-object! % screen-size delta-time))
    (update p :timer #(+ % delta-time))
    (assoc p :remove? (let [{:keys [timer time]} p]
                        (> timer time)))))

(defn update-particles
  [particles screen-size delta-time]
  (->> particles
       (map #(update-particle % screen-size delta-time))
       (filter :remove?)
       (into #{})))

(def particle-color [1 1 1 1])

(defn draw-particle
  [particle shape-renderer]
  (let [[r g b a] particle-color
        {:keys [space-object]} particle
        {[width height] :size
         [x y]          :pos} space-object]
    (.setColor shape-renderer r g b a)
    (.begin shape-renderer ShapeRenderer$ShapeType/Line)
    (.circle shape-renderer (- x (/ width 2)) (- y (/ height 2)) (/ width 2))
    (.end shape-renderer)))

(defn draw-particles
  [particles shape-renderer]
  (doseq [particle particles]
    (draw-particle particle shape-renderer)))
