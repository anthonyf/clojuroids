(ns clojuroids.jukebox
  (:import [com.badlogic.gdx Gdx]
           [com.badlogic.gdx.audio Sound]))

(def sounds (atom {}))

(defn load-sound!
  [name path]
  (swap! sounds assoc name
         {:sound (.newSound Gdx/audio (.internal Gdx/files path))
          :looping? false}))

(defn play-sound
  [name]
  (.play (-> @sounds name :sound))
  (swap! sounds assoc-in [name :looping?] false))

(defn sound-looping?
  [name]
  (-> @sounds name :looping?))

(defn loop-sound
  [name]
  (when-not (sound-looping? name)
    (.loop (-> @sounds name :sound))
    (swap! sounds assoc-in [name :looping?] true)))

(defn stop-sound
  [name]
  (.stop (-> @sounds name :sound))
  (swap! sounds assoc-in [name :looping?] false))

(defn stop-all
  [name]
  (doseq [[name sound] @sounds]
    (stop-sound name sound)))
