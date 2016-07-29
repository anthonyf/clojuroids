(ns clojuroids.core
  (:require [clojuroids.key-state :refer :all])
  (:import [com.badlogic.gdx ApplicationListener Gdx]
           [com.badlogic.gdx.graphics GL30 OrthographicCamera]))

(def app-listener
  (let [camera-ref (atom nil)
        screen-size-ref (atom [])
        key-state-ref (atom {})]
    (reify
      ApplicationListener

      (create [this]
        (reset! screen-size-ref [(.getWidth Gdx/graphics)
                                 (.getHeight Gdx/graphics)])
        (let [[width height] @screen-size-ref]
          (reset! camera-ref (OrthographicCamera. width height))
          (.translate @camera-ref (/ width 2) (/ height 2))
          (.update @camera-ref))

        (.setInputProcessor Gdx/input (make-input-processor key-state-ref)))

      (render [this]
        (.glClearColor Gdx/gl 0 0 0 1)
        (.glClear Gdx/gl GL30/GL_COLOR_BUFFER_BIT)

        (swap! key-state-ref update-key-state))

      (resize [this width height])
      (pause [this])
      (resume [this])
      (dispose [this]))))
