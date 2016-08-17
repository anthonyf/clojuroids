(ns clojuroids.common
  (:import [com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
            FreeTypeFontGenerator$FreeTypeFontParameter]
           [com.badlogic.gdx Gdx]))

(def title "Clojuroids")

(def screen-size [500 400])

(def camera (atom nil))

(defn gen-font [size color]
  (let [gen (FreeTypeFontGenerator.
             (.internal Gdx/files
                        "fonts/Hyperspace Bold.ttf"))]
    (.generateFont
     gen
     (doto (FreeTypeFontGenerator$FreeTypeFontParameter.)
       (-> .size (set! size))
       (-> .color (set! color))))))
