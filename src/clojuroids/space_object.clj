(ns clojuroids.space-object
  (:require [clojuroids.common :as c])
  (:import [com.badlogic.gdx.graphics.glutils
            ShapeRenderer ShapeRenderer$ShapeType]
           [com.badlogic.gdx.math Polygon MathUtils]))

(defrecord SpaceObject
    [pos dpos radians rotation-speed shape])

(defn make-vector [direction magnitude]
  [(* (MathUtils/cos direction) magnitude)
   (* (MathUtils/sin direction) magnitude)])

(defn vector-add [[x1 y1] [x2 y2]]
  [(+ x1 x2) (+ y1 y2)])

(defn wrap
  [space-object]
  (let [[screen-width screen-height] c/screen-size
        {[x y] :pos} space-object]
    (assoc space-object :pos
           [(cond (< x 0) screen-width
                  (> x screen-width) 0
                  :else x)
            (cond (< y 0) screen-height
                  (> y screen-height) 0
                  :else y)])))

(defn move
  [space-object delta-time]
  (let [{[x y]   :pos
         [dx dy] :dpos} space-object]
    (assoc space-object :pos
           [(+ x (* dx delta-time))
            (+ y (* dy delta-time))])))

(defn vert-lines-seq
  "returns a sequence of line segments for verts"
  [verts]
  (let [verts-count (count verts)]
    (for [i (range verts-count)]
      [(nth verts i)
       (nth verts (mod (dec i) verts-count))])))

(defn draw-lines
  [shape-renderer lines [r g b a]]
  (.setColor shape-renderer r g b a)
  (.begin shape-renderer ShapeRenderer$ShapeType/Line)
  (doseq [[[x1 y1]
           [x2 y2]] lines]
    (.line shape-renderer x1 y1 x2 y2))
  (.end shape-renderer))

(defn draw-shape
  [shape-renderer shape color]
  (draw-lines shape-renderer (vert-lines-seq shape) color))

(defn draw-space-object
  [space-object shape-renderer color]
  (let [{shape :shape} space-object]
    (draw-shape shape-renderer shape color)))

(defn update-space-object!
  [space-object delta-time]
  (-> space-object
      (move delta-time)
      (wrap)))

(defn make-space-object
  [& {:keys [pos dpos radians rotation-speed shape] :or
      {pos [0 0]
       dpos [0 0]
       radians 0
       rotation-speed 3
       shape []}}]
  (->SpaceObject pos dpos radians rotation-speed shape))

