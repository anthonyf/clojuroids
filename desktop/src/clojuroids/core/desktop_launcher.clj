(ns clojuroids.core.desktop-launcher
  (:require [clojuroids.core :refer :all]
            [clojuroids.common :as c])
  (:import [com.badlogic.gdx.backends.lwjgl
            LwjglApplication
            LwjglApplicationConfiguration]
           [com.badlogic.gdx ApplicationListener]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (let [[width height] c/screen-size
        config (doto (LwjglApplicationConfiguration.)
                 (-> .title (set! "clojuroids"))
                 (-> .width (set! width))
                 (-> .height (set! height))
                 (-> .resizable (set! false)))]
    (LwjglApplication. app-listener config)
    (Keyboard/enableRepeatEvents true)))
