(ns clojuroids.core
  (:require [clojuroids.common :as c]
            [clojuroids.key-state :as k]
            [clojuroids.game-state-manager :as gsm]
            [clojuroids.play-state :as ps]
            [clojuroids.jukebox :as j]
            [clojuroids.menu-state :as ms])
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
  (reify
    ApplicationListener

    (create [this]
      (let [[width height] c/screen-size]
        (reset! c/camera (OrthographicCamera. width height))
        (.translate @c/camera (/ width 2) (/ height 2))
        (.update @c/camera))

      (k/init-key-state)

      (doseq [[sym path] sounds]
        (j/load-sound! sym path))

      (gsm/register-game-state! :menu ms/make-menu-state)
      (gsm/register-game-state! :play ps/make-play-state)

      (gsm/set-state! :menu))

    (render [this]
      (.glClearColor Gdx/gl 0 0 0 1)
      (.glClear Gdx/gl GL30/GL_COLOR_BUFFER_BIT)
      (gsm/update-game-state!)
      (k/update-key-state!))

    (resize [this width height])
    (pause [this])
    (resume [this])
    (dispose [this])))
