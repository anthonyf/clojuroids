(ns clojuroids.core
  (:require [clojuroids.common :as c])
  (:import [com.badlogic.gdx ApplicationListener Gdx]
           [com.badlogic.gdx.graphics GL30]
           [com.badlogic.gdx.backends.lwjgl
            LwjglApplication
            LwjglApplicationConfiguration]
           [org.lwjgl.input Keyboard]))

(def app-listener
  (reify
    ApplicationListener

    (create [this])

    (render [this]
      (.glClearColor Gdx/gl 0 0 0 1)
      (.glClear Gdx/gl GL30/GL_COLOR_BUFFER_BIT))

    (resize [this width height])
    (pause [this])
    (resume [this])
    (dispose [this])))

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
