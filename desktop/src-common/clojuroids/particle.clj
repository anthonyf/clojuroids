(ns clojuroids.particle
  (:require [clojuroids.space-object :as so]))


(defrecord Particle
    [space-object timer time remove?])

(defn make-particle
  [[x y]]
  (map->Particle {:space-object (so/make-space-object [x y] 0)}))
