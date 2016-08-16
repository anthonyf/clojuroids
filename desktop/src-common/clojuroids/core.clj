(ns clojuroids.core
  (:require [clojuroids.common :as c]
            [clojuroids.key-state :refer :all]
            [clojuroids.game-state-manager :as gsm]
            [clojuroids.play-state :as ps]
            [clojuroids.jukebox :as j]
            [clojure.stacktrace :as st])
  (:import [com.badlogic.gdx ApplicationListener Gdx]
           [com.badlogic.gdx.graphics GL30 OrthographicCamera]
           (com.badlogic.gdx Input$Keys)))

(defprotocol Restartable
  (restart [this]))

(def rewind-factor 3)

(def app-listener
  (let [camera-ref (atom nil)
        game-state-ref (atom nil)
        game-states-ref (atom '())
        restart? (atom false)]
    (reify
      ApplicationListener

      (create [this]
        (let [[width height] c/screen-size]
          (reset! camera-ref (OrthographicCamera. width height))
          (.translate @camera-ref (/ width 2) (/ height 2))
          (.update @camera-ref))

        (init-key-state)

        (j/load-sound! :explode "sounds/explode.ogg")
        (j/load-sound! :extralife "sounds/extralife.ogg")
        (j/load-sound! :largesaucer "sounds/largesaucer.ogg")
        (j/load-sound! :pulsehigh "sounds/pulsehigh.ogg")
        (j/load-sound! :pulselow "sounds/pulselow.ogg")
        (j/load-sound! :saucershoot "sounds/saucershoot.ogg")
        (j/load-sound! :shoot "sounds/shoot.ogg")
        (j/load-sound! :smallsaucer "sounds/smallsaucer.ogg")
        (j/load-sound! :thruster "sounds/thruster.ogg")

        (swap! game-state-ref gsm/set-state ps/make-play-state))

      (render [this]
        (.glClearColor Gdx/gl 0 0 0 1)
        (.glClear Gdx/gl GL30/GL_COLOR_BUFFER_BIT)

        (when @restart?
          (reset! restart? false)
          (swap! game-state-ref gsm/set-state ps/make-play-state))

        (try
          (when-not (nil? @game-state-ref)
            (if (key-down? Input$Keys/BACKSPACE)
              (when (not (empty? @game-states-ref))
                (reset! game-state-ref (first (take rewind-factor @game-states-ref)))
                (reset! game-states-ref (drop rewind-factor @game-states-ref))
                (gsm/draw @game-state-ref))
              (do (swap! game-state-ref gsm/handle-input)
                  (swap! game-state-ref gsm/update! (.getDeltaTime Gdx/graphics))
                  (gsm/draw @game-state-ref)
                  (update-key-state)
                  (swap! game-states-ref conj @game-state-ref))))
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
