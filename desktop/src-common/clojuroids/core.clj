(ns clojuroids.core
  (:import [com.badlogic.gdx ApplicationListener
            Gdx]
           [com.badlogic.gdx.graphics GL30]))

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
