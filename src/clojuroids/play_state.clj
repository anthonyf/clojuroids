(ns clojuroids.play-state
  (:require [clojuroids.game-state-manager :as gsm]))

(defrecord PlayState []
  gsm/GameState

  (init [this]
    (println "PlayState init")

    this)

  (update! [this delta-time]
    (println "PlayState update")
    this)

  (draw [this]
    (println "PlayState draw"))

  (dispose [this]
    (println "PlayState dispose")))

(defn make-play-state
  []
  (map->PlayState {}))
