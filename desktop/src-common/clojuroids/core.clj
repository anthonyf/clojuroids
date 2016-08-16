(ns clojuroids.core
  (:require [clojuroids.common :as c]
            [clojuroids.key-state :as k]
            [clojuroids.game-state-manager :as gsm]
            [clojuroids.play-state :as ps]
            [clojuroids.jukebox :as j])
  (:import [com.badlogic.gdx ApplicationListener Gdx]
           [com.badlogic.gdx.graphics GL30 OrthographicCamera]))

(def sounds [[:explode "sounds/explode.ogg"]
             [:extralife "sounds/extralife.ogg"]
             [:largesaucer "sounds/largesaucer.ogg"]
             [:pulsehigh "sounds/pulsehigh.ogg"]
             [:pulselow "sounds/pulselow.ogg"]
             [:saucershoot "sounds/saucershoot.ogg"]
             [:shoot "sounds/shoot.ogg"]
             [:smallsaucer "sounds/smallsaucer.ogg"]
             [:thruster "sounds/thruster.ogg"]])

(def app-listener
  (let [camera-ref (atom nil)]
    (reify
      ApplicationListener

      (create [this]
        (let [[width height] c/screen-size]
          (reset! camera-ref (OrthographicCamera. width height))
          (.translate @camera-ref (/ width 2) (/ height 2))
          (.update @camera-ref))

        (k/init-key-state)

        (doseq [[sym path] sounds]
          (j/load-sound! sym path))

        (gsm/set-state! (ps/make-play-state)))

      (render [this]
        (.glClearColor Gdx/gl 0 0 0 1)
        (.glClear Gdx/gl GL30/GL_COLOR_BUFFER_BIT)
        (gsm/update-game-state)
        (k/update-key-state))

      (resize [this width height])
      (pause [this])
      (resume [this])
      (dispose [this]))))
