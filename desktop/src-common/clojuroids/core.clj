(ns clojuroids.core
  (:require [clojuroids.common :as c]
            [clojuroids.key-state :as k]
            [clojuroids.game-state-manager :as gsm]
            [clojuroids.play-state :as ps]
            [clojuroids.jukebox :as j]
            [clojuroids.menu-state :as ms]
            [clojuroids.high-scores-state :as hs]
            [clojuroids.game-over-state :as gos])
  (:import [com.badlogic.gdx ApplicationListener Gdx]
           [com.badlogic.gdx.graphics GL30 OrthographicCamera]
           [com.badlogic.gdx.utils.viewport FitViewport]
           [com.badlogic.gdx.graphics.glutils ShapeRenderer ShapeRenderer$ShapeType]))

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
  (let [shape-renderer (atom nil)
        viewport (atom nil)]
    (reify
      ApplicationListener

      (create [this]
        (let [[width height] c/screen-size]
          (reset! shape-renderer (ShapeRenderer.))
          (reset! c/camera (OrthographicCamera. width height))
          (reset! viewport (FitViewport. width height @c/camera))
          (.translate @c/camera (/ width 2) (/ height 2))
          (.update @c/camera))

        (k/init-key-state)

        (doseq [[sym path] sounds]
          (j/load-sound! sym path))

        (gsm/register-game-state! :menu ms/make-menu-state)
        (gsm/register-game-state! :play ps/make-play-state)
        (gsm/register-game-state! :high-scores hs/make-high-scores-state)
        (gsm/register-game-state! :game-over gos/make-game-over-state)

        (gsm/set-state! :menu))

      (render [this]
        (.glClearColor Gdx/gl 0 0 0 1)
        (.glClear Gdx/gl GL30/GL_COLOR_BUFFER_BIT)

        (.setProjectionMatrix @shape-renderer (.combined @c/camera))

        ;; draw a box around the game screen
        (.begin @shape-renderer ShapeRenderer$ShapeType/Line)
        (let [[sw sh] c/screen-size]
          (.line @shape-renderer 1 1 sw 1)
          (.line @shape-renderer sw 1 sw sh)
          (.line @shape-renderer sw sh 1 sh)
          (.line @shape-renderer 1 sh 1 1))
        (.end @shape-renderer)

        (gsm/update-game-state!)
        (k/update-key-state!))

      (resize [this width height]
        (.update @viewport width height true))

      (pause [this])
      (resume [this])
      (dispose [this]
        (.dispose @shape-renderer)))))
