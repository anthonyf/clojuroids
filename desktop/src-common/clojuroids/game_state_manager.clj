(ns clojuroids.game-state-manager
  (:require [clojure.stacktrace :as st]
            [clojuroids.key-state :as k])
  (:import [com.badlogic.gdx Input$Keys]
           [com.badlogic.gdx Gdx]))

(def game-states (atom {}))
(def current-state (atom nil))
(def state-history (atom ()))
(def rewind-factor 3)
(def restartable? false)

(defprotocol GameState
  "protocol for game states"
  (init [this])
  (draw [this])
  (update! [this delta-time])
  (handle-input [this])
  (dispose [this]))

(defn register-game-state!
  [key constructor]
  (swap! game-states assoc key constructor))

(defn set-state!
  [key]
  (when-not (nil? @current-state)
    (dispose @current-state))
  (reset! current-state (init ((key @game-states)))))

(defn update-game-state!
  []
  (try
    (when-not (nil? @current-state)
      (if (k/key-down? Input$Keys/BACKSPACE)
        (when (not (empty? @state-history))
          (reset! current-state (first (take rewind-factor @state-history)))
          (reset! state-history (drop rewind-factor @state-history))
          (draw @current-state))
        (do (swap! current-state handle-input)
            (swap! current-state update! (.getDeltaTime Gdx/graphics))
            (draw @current-state)
            (swap! state-history conj @current-state))))
    (catch Exception e
      (if restartable?
        (do (reset! current-state nil)
            (st/print-cause-trace e))
        (throw e)))))
