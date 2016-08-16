(ns clojuroids.asteroids
  (:require [clojuroids.space-object :as so])
  (:import [com.badlogic.gdx.math MathUtils]))

(defrecord Asteroid
  [space-object type num-points dists remove?])

(defn- update-asteroid-shape
  [asteroid]
  (let [{:keys [num-points dists]
         {[x y] :pos
          :keys [radians]} :space-object} asteroid
        angles (take num-points
                     (drop 1 (iterate
                              (fn [angle]
                                (+ angle (/ (* 2 Math/PI) num-points)))
                              0)))]
    (assoc-in asteroid [:space-object :shape]
              (map (fn [angle dist]
                     [(+ x (* (MathUtils/cos (+ radians angle)) dist))
                      (+ y (* (MathUtils/sin (+ radians angle)) dist))])
                   angles dists))))

(defn- update-asteroid
  [asteroid delta-time]
  (let [{space-object :space-object} asteroid
        {:keys [rotation-speed]} space-object]
    (-> asteroid
      (assoc :space-object (so/update-space-object! space-object delta-time))
      (update-asteroid-shape)
      (assoc :radians (* rotation-speed delta-time)))))

(defn update-asteroids
  [asteroids delta-time]
  (->> asteroids
       (map #(update-asteroid % delta-time))
       (remove :remove?)
       (into #{})))

(def asteroid-color [1 1 1 1])

(defn- draw-asteroid
  [asteroid shape-renderer]
  (let [{:keys [space-object]} asteroid]
    (so/draw-space-object space-object shape-renderer asteroid-color)))

(defn draw-asteroids
  [asteroids shape-renderer]
  (doseq [asteroid asteroids]
    (draw-asteroid asteroid shape-renderer)))

(defn make-asteroid
  [[x y] type]
  (let [radians (MathUtils/random (* 2 Math/PI))
        {:keys [num-points size speed]} (case type
                                          :small {:num-points 8
                                                  :size       [12 12]
                                                  :speed      (MathUtils/random 70 100)}
                                          :medium {:num-points 10
                                                   :size       [20 20]
                                                   :speed      (MathUtils/random 50 60)}
                                          :large {:num-points 12
                                                  :size       [40 40]
                                                  :speed      (MathUtils/random 20 30)})]
    (-> (map->Asteroid {:type         type
                        :num-points   num-points
                        :dists        (let [[width _] size
                                            radius (/ width 2.0)]
                                        (for [_ (range num-points)]
                                          (MathUtils/random (/ radius 2.0) radius)))
                        :space-object (so/map->SpaceObject {:pos            [x y]
                                                            :rotation-speed (MathUtils/random -1 1)
                                                            :radians        radians
                                                            :size           size
                                                            :dpos           [(* speed (MathUtils/cos radians))
                                                                             (* speed (MathUtils/sin radians))]})})
        (update-asteroid-shape))))

(defn score
  [asteroid]
  (let [{type :type} asteroid]
    (case type
      :small 100
      :medium 50
      :large 20)))
