(ns clojuroids.jukebox
  (:import [com.badlogic.gdx Gdx]
           [com.badlogic.gdx.audio Sound]))

(def sounds (atom {}))

(defn load-sound!
  [name path]
  (swap! sounds assoc name
         (.newSound Gdx/audio (.internal Gdx/files path))))

(defn play-sound
  [name]
  (.play (name @sounds)))

(defn loop-sound
  [name]
  (.loop (name @sounds)))

(defn stop-sound
  [name]
  (.stop (name @sounds)))

(defn stop-all
  [name]
  (doseq [[name sound] @sounds]
    (stop-sound name sound)))
