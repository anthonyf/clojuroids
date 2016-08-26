(ns clojuroids.game-state-manager
  (:require [clojure.stacktrace :as st]
            [clojuroids.key-state :as k])
  (:import [com.badlogic.gdx Input$Keys]
           [com.badlogic.gdx Gdx]))

(def game-states (atom {}))
(def current-state (atom nil))
(def next-state (atom nil))
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
  [key & args]
  (reset! next-state {:key key :args args}))

(defrecord ErrorState []
  GameState
  (init [this] init)
  (draw [this])
  (update! [this delta-time] this)
  (dispose [this]))

(defn update-game-state!
  []
  (try
    (when-not (nil? @next-state)
      (when-not (nil? @current-state)
        (dispose @current-state))
      (let [{:keys [key args]} @next-state]
        (reset! current-state (init (apply (key @game-states)
                                           args))))
      (reset! next-state nil))
    (swap! current-state update! (.getDeltaTime Gdx/graphics))
    (draw @current-state)
    (catch Exception e
      (if restartable?
        (do (reset! current-state (ErrorState.))
            (st/print-cause-trace e))
        (throw e)))))
