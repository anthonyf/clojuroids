(ns clojuroids.game-state-manager)

(defprotocol game-state
  "protocol for game states"
  (init [this])
  (draw [this])
  (update! [this delta-time])
  (handle-input [this])
  (dispose [this]))

(defn set-state
  [state new-state & args]
  (when-not (nil? state)
    (dispose state))
  (init (apply new-state args)))
