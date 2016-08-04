(ns clojuroids.core
  (:require [clojuroids.key-state :refer :all]
            [clojuroids.game-state-manager :as gsm]
            [clojuroids.play-state :as ps]
            [clojure.stacktrace :as st])
  (:import [com.badlogic.gdx ApplicationListener Gdx]
           [com.badlogic.gdx.graphics GL30 OrthographicCamera]))

(defprotocol Restartable
  (restart [this]))

(def app-listener
  (let [camera-ref (atom nil)
        screen-size-ref (atom [])
        key-state-ref (atom {})
        game-state-ref (atom nil)
        restart? (atom false)]
    (reify
      ApplicationListener

      (create [this]
        (reset! screen-size-ref [(.getWidth Gdx/graphics)
                                 (.getHeight Gdx/graphics)])
        (let [[width height] @screen-size-ref]
          (reset! camera-ref (OrthographicCamera. width height))
          (.translate @camera-ref (/ width 2) (/ height 2))
          (.update @camera-ref))

        (.setInputProcessor Gdx/input (make-input-processor key-state-ref))
        (swap! game-state-ref gsm/set-state ps/make-play-state screen-size-ref key-state-ref))

      (render [this]
        (.glClearColor Gdx/gl 0 0 0 1)
        (.glClear Gdx/gl GL30/GL_COLOR_BUFFER_BIT)

        (when @restart?
          (reset! restart? false)
          (swap! game-state-ref gsm/set-state ps/make-play-state screen-size-ref key-state-ref))

        (try
          (when-not (nil? @game-state-ref)
            (swap! game-state-ref gsm/handle-input)
            (swap! game-state-ref gsm/update! @screen-size-ref (.getDeltaTime Gdx/graphics))
            (gsm/draw @game-state-ref)
            (swap! key-state-ref update-key-state))
          (catch Exception e
            (reset! game-state-ref nil)
            (st/print-cause-trace e))))

      (resize [this width height])
      (pause [this])
      (resume [this])
      (dispose [this])

      Restartable
      (restart [this]
        (reset! restart? true)))))
