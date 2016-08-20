(ns clojuroids.common
  (:import [com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
            FreeTypeFontGenerator$FreeTypeFontParameter]
           [com.badlogic.gdx.graphics.g2d GlyphLayout]
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


(def glyph-layout (GlyphLayout.))

(defn draw-centered-text
  [sprite-batch font text height]
  (let [[sw _] screen-size]
    (.draw font sprite-batch text
           (float (/ (- sw (.width (doto glyph-layout
                                     (.setText font text))))
                     2))
           (float height))))
