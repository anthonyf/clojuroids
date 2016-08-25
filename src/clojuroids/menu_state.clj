(ns clojuroids.menu-state
  (:require [clojuroids.game-state-manager :as gsm]
            [clojuroids.common :as c]
            [clojuroids.key-state :as k]
            [clojuroids.asteroids :as a])
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch]
           [com.badlogic.gdx.graphics.glutils ShapeRenderer]
           [com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
            FreeTypeFontGenerator$FreeTypeFontParameter]
           [com.badlogic.gdx.graphics Color]
           [com.badlogic.gdx Gdx Input$Keys]
           [com.badlogic.gdx.math MathUtils]))

(declare play high-scores quit)

(defrecord MenuState [sprite-batch title-font item-font current-item menu-items
                      asteroids shape-renderer]
  gsm/GameState

  (init [this]
    (let [[screen-width screen-height] c/screen-size]
      (-> this
          (merge {:sprite-batch (SpriteBatch.)
                  :shape-renderer (ShapeRenderer.)
                  :title-font   (c/gen-font 56 Color/WHITE)
                  :item-font    (c/gen-font 20 Color/WHITE)
                  :menu-items   [{:text "Play" :action play}
                                 {:text "High Scores" :action high-scores}
                                 {:text "Quit" :action quit}]
                  :asteroids    (for [_ (range 6)]
                                  (a/make-asteroid [(MathUtils/random screen-width)
                                                    (MathUtils/random screen-height)]
                                                   :large))
                  :current-item 0}))))

  (update! [this delta-time]
    (as-> this this
      (update this :asteroids a/update-asteroids delta-time)
      (cond
        ;; menu up
        (and (k/key-pressed? Input$Keys/UP)
             (> current-item 0))
        (update this :current-item - 1)

        ;; menu down
        (and (k/key-pressed? Input$Keys/DOWN)
             (< current-item (dec (count menu-items))))
        (update this :current-item + 1)

        ;; menu select
        (k/key-pressed? Input$Keys/ENTER)
        (let [{:keys [action]} (menu-items current-item)]
          (action this)
          this)

        :else this)))

  (draw [this]
    (.setProjectionMatrix sprite-batch (.combined @c/camera))
    (.setProjectionMatrix shape-renderer (.combined @c/camera))

    (a/draw-asteroids asteroids shape-renderer)

    (.begin sprite-batch)

    (c/draw-centered-text sprite-batch title-font c/title 300)

    (dotimes [i (count menu-items)]
      (let [{:keys [text]} (nth menu-items i)]
        (if (= current-item i)
          (.setColor item-font Color/RED)
          (.setColor item-font Color/WHITE))
        (c/draw-centered-text sprite-batch item-font text (- 180 (* 35 i)))))

    (.end sprite-batch))

  (dispose [this]
    (.dispose sprite-batch)
    (.dispose shape-renderer)
    (.dispose title-font)
    (.dispose item-font)))

(defn make-menu-state
  []
  (map->MenuState {}))

(defn play [menu-state]
  (gsm/set-state! :play))

(defn high-scores [menu-state]
  (gsm/set-state! :high-scores))

(defn quit [menu-state]
  (.exit Gdx/app))
