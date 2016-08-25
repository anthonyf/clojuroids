(ns clojuroids.key-state
  (:import [com.badlogic.gdx ApplicationListener Gdx Input$Keys InputAdapter]))

(def key-state (atom {}))

(defn init-key-state
  []
  (.setInputProcessor Gdx/input
                      (proxy [InputAdapter] []
                        (keyDown [key]
                          (swap! key-state assoc-in [:keys key] true)
                          true)
                        (keyUp [key]
                          (swap! key-state assoc-in [:keys key] false)
                          true))))

(defn update-key-state! []
  (swap! key-state assoc :pkeys (:keys @key-state)))

(defn key-down? [key]
  (get-in @key-state [:keys key] false))

(defn key-pressed? [key]
  (and (get-in @key-state [:keys key] false)
       (not (get-in @key-state [:pkeys key] false))))
