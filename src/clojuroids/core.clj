(ns clojuroids.core
  (:require [clojuroids.common :as c]
            [clojuroids.key-state :as k]
            [clojuroids.game-state-manager :as gsm]
            [clojuroids.play-state :as ps])
  (:import [com.badlogic.gdx ApplicationListener Gdx]
           [com.badlogic.gdx.graphics GL30 OrthographicCamera]
           [com.badlogic.gdx.backends.lwjgl
            LwjglApplication
            LwjglApplicationConfiguration]
           [org.lwjgl.input Keyboard]
           [com.badlogic.gdx.utils.viewport FitViewport]
           [com.badlogic.gdx.graphics.glutils ShapeRenderer ShapeRenderer$ShapeType]))

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

        (gsm/register-game-state! :play ps/make-play-state)

        (gsm/set-state! :play))

      (render [this]
        (.glClearColor Gdx/gl 0 0 0 1)
        (.glClear Gdx/gl GL30/GL_COLOR_BUFFER_BIT)

        (.setProjectionMatrix @shape-renderer (.combined @c/camera))

        ;; draw a box around the game screen
        (.begin @shape-renderer ShapeRenderer$ShapeType/Line)
        (let [[sw sh] c/screen-size
              sw (- sw 1)
              sh (- sh 1)]
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

(defn -main
  []
  (let [[width height] c/screen-size
        config (doto (LwjglApplicationConfiguration.)
                 (-> .title (set! c/title))
                 (-> .width (set! width))
                 (-> .height (set! height))
                 (-> .resizable (set! true)))]
    (LwjglApplication. app-listener config)
    (Keyboard/enableRepeatEvents true)))
