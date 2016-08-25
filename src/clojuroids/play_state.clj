(ns clojuroids.play-state
  (:require [clojuroids.game-state-manager :as gsm]))

(defrecord PlayState []
  gsm/GameState

  (init [this]
    (println "PlayState init")

    this)

  (draw [this]
    (println "PlayState draw"))

  (update! [this delta-time]
    (println "PlayState update")
    this)

  (dispose [this]
    (println "PlayState dispose")))

(defn make-play-state
  []
  (map->PlayState {}))

