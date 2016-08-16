(ns clojuroids.game-state-manager
  (:require [clojure.stacktrace :as st]
            [clojuroids.key-state :as k])
  (:import [com.badlogic.gdx Input$Keys]
           [com.badlogic.gdx Gdx]))

(def game-state (atom nil))
(def game-states (atom ()))
(def restartable? false)
(def rewind-factor 3)

(defprotocol GameState
  "protocol for game states"
  (init [this])
  (draw [this])
  (update! [this delta-time])
  (handle-input [this])
  (dispose [this]))

(defn set-state!
  [new-state]
  (when-not (nil? @game-state)
    (dispose @game-state))
  (reset! game-state (init new-state)))

(defn update-game-state
  []
  (try
    (when-not (nil? @game-state)
      (if (k/key-down? Input$Keys/BACKSPACE)
        (when (not (empty? @game-states))
          (reset! game-state (first (take rewind-factor @game-states)))
          (reset! game-states (drop rewind-factor @game-states))
          (draw @game-state))
        (do (swap! game-state handle-input)
            (swap! game-state update! (.getDeltaTime Gdx/graphics))
            (draw @game-state)
            (swap! game-states conj @game-state))))
    (catch Exception e
      (if restartable?
        (do (reset! game-state nil)
            (st/print-cause-trace e))
        (throw e)))))
