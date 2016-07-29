(ns clojuroids.key-state
  (:import [com.badlogic.gdx ApplicationListener Gdx Input$Keys InputAdapter]))

(defn- key-down
  [key-state key]
  (assoc-in key-state [:keys key] true))

(defn- key-up
  [key-state key]
  (assoc-in key-state [:keys key] false))

(defn make-input-processor
  [state]
  (proxy [InputAdapter] []
    (keyDown [key]
      (swap! state key-down key)
      true)
    (keyUp [key]
      (swap! state key-up key)
      true)))

(defn update-key-state [state]
  (assoc state :pkeys (:keys state)))

(defn key-down? [state key]
  (get-in state [:keys key] false))

(defn key-pressed? [state key]
  (and (get-in state [:keys key] false)
       (not (get-in state [:pkeys key] false))))
