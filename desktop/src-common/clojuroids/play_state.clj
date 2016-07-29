(ns clojuroids.play-state
  (:require [clojuroids.game-state-manager :as gsm])
  (:gen-class))

(defrecord PlayState [screen-size-ref key-state-ref shape-renderer player]
  gsm/game-state
  (init [this]
    (println "PlayState init")
    this)
  (draw [this]
    (println "PlayState draw"))
  (update! [this screen-size delta-time]
    (println "PlayState update")
    this)
  (handle-input [this]
    (println "PlayState handle-input")
    this)
  (dispose [this]
    (println "PlayState dispose")))

(defn make-play-state
  [screen-size-ref key-state-ref]
  (map->PlayState {:screen-size-ref screen-size-ref
                   :key-state-ref key-state-ref}))
