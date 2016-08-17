(ns clojuroids.game-state-manager
  (:require [clojure.stacktrace :as st]
            [clojuroids.key-state :as k])
  (:import [com.badlogic.gdx Input$Keys]
           [com.badlogic.gdx Gdx]))

(def game-states (atom {}))
(def current-state (atom nil))
(def next-state-key (atom nil))
(def restartable? false)

(defprotocol GameState
  "protocol for game states"
  (init [this])
  (draw [this])
  (update! [this delta-time])
  (dispose [this]))

(defn register-game-state!
  [key constructor]
  (swap! game-states assoc key constructor))

(defn set-state!
  [key]
  (reset! next-state-key key))

(defn update-game-state!
  []
  (try
    (when-not (nil? @current-state)
      (swap! current-state update! (.getDeltaTime Gdx/graphics))
      (draw @current-state))
    (when-not (nil? @next-state-key)
        (when-not (nil? @current-state)
          (dispose @current-state))
        (reset! current-state (init ((@next-state-key @game-states))))
        (reset! next-state-key nil))
    (catch Exception e
      (if restartable?
        (do (reset! current-state nil)
            (st/print-cause-trace e))
        (throw e)))))
