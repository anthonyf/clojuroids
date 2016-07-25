(ns clojuroids.core.desktop-launcher
  (:require [clojuroids.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl
            LwjglApplication
            LwjglApplicationConfiguration]
           [com.badlogic.gdx ApplicationListener]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (let [config (doto (LwjglApplicationConfiguration.)
                 (-> .title (set! "clojuroids"))
                 (-> .width (set! 500))
                 (-> .height (set! 400))
                 (-> .resizable (set! false)))]
    (LwjglApplication. app-listener config)
    (Keyboard/enableRepeatEvents true)))
