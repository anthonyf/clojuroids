(ns clojuroids.menu-state
  (:require [clojuroids.game-state-manager :as gsm]
            [clojuroids.common :as c]
            [clojuroids.key-state :as k])
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch GlyphLayout]
           [com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
            FreeTypeFontGenerator$FreeTypeFontParameter]
           [com.badlogic.gdx.graphics Color]
           [com.badlogic.gdx Gdx Input$Keys]))

(declare play highscores quit)

(defrecord MenuState [sprite-batch title-font item-font glyph-layout current-item menu-items]
  gsm/GameState

  (init [this]
    (-> this
        (merge {:sprite-batch (SpriteBatch.)
                :glyph-layout (GlyphLayout.)
                :title-font   (c/gen-font 56 Color/WHITE)
                :item-font    (c/gen-font 20 Color/WHITE)
                :menu-items   [{:text "Play" :action play}
                               {:text "Highscores" :action highscores}
                               {:text "Quit" :action quit}]
                :current-item 0})))

  (update! [this delta-time]
    (cond
      ;; menu up
      (and (k/key-pressed? Input$Keys/UP)
           (> current-item 0))
      (update this :current-item #(- % 1))

      ;; menu down
      (and (k/key-pressed? Input$Keys/DOWN)
           (< current-item (dec (count menu-items))))
      (update this :current-item #(+ % 1))

      ;; menu select
      (k/key-pressed? Input$Keys/ENTER)
      (let [{:keys [action]} (menu-items current-item)]
        (action this)
        this)

      :else this))

  (draw [this]
    (.setProjectionMatrix sprite-batch (.combined @c/camera))
    (.begin sprite-batch)
    (let [[sw _]      c/screen-size
          title-width (.width (doto glyph-layout
                                (.setText title-font c/title)))]
      ;; draw title
      (.draw title-font
             sprite-batch
             c/title
             (float (/ (- sw title-width) 2))
             (float 300))
      ;; draw menu items
      (dotimes [i (count menu-items)]
        (let [{:keys [text]} (nth menu-items i)
              item-width     (.width (doto glyph-layout
                                       (.setText item-font text)))]
          (if (= current-item i)
            (.setColor item-font Color/RED)
            (.setColor item-font Color/WHITE))
          (.draw item-font
                 sprite-batch
                 text
                 (float (/ (- sw item-width) 2))
                 (float (- 180 (* 35 i)))))))
    (.end sprite-batch))

  (dispose [this]
    (.dispose sprite-batch)
    (.dispose title-font)
    (.dispose item-font)))

(defn make-menu-state
  []
  (map->MenuState {}))

(defn play [menu-state]
  (gsm/set-state! :play))

(defn highscores [menu-state]
  (gsm/set-state! :highscores))

(defn quit [menu-state]
  (.exit Gdx/app))
