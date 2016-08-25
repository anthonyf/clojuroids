(ns clojuroids.game-over-state
  (:require [clojure.string :as str]
            [clojuroids.game-state-manager :as gsm]
            [clojuroids.common :as c]
            [clojuroids.key-state :as k]
            [clojuroids.game-data :as gd])
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch]
           [com.badlogic.gdx.graphics.glutils ShapeRenderer ShapeRenderer$ShapeType]
           [com.badlogic.gdx.graphics Color]
           [com.badlogic.gdx Input$Keys]))

(def letters (conj (mapv #(char (+ % (int \A))) (range 26))
                   \space))

(defrecord GameOverState [sprite-batch shape-renderer
                          tentative-score letter-indexes current-char
                          title-font font]
  gsm/GameState
  (init [this]
    (-> this
        (merge {:sprite-batch (SpriteBatch.)
                :shape-renderer (ShapeRenderer.)
                :letter-indexes [0 0 0]
                :current-char 0
                :title-font (c/gen-font 32 Color/WHITE)
                :font (c/gen-font 20 Color/WHITE)})))

  (update! [this delta-time]
    (cond (k/key-pressed? Input$Keys/ENTER)
          (do (when (gd/high-score? tentative-score)
                (gd/add-high-score (str/join (map (partial letters) letter-indexes))
                                   tentative-score))
              (gsm/set-state! :menu)
              this)

          (k/key-pressed? Input$Keys/UP)
          (update-in this [:letter-indexes current-char]
                     (fn [i] (mod (inc i) (count letters))))

          (k/key-pressed? Input$Keys/DOWN)
          (update-in this [:letter-indexes current-char]
                     (fn [i] (mod (dec i) (count letters))))

          (and (k/key-pressed? Input$Keys/RIGHT)
               (< current-char (dec (count letter-indexes))))
          (update this :current-char inc)

          (and (k/key-pressed? Input$Keys/LEFT)
               (> current-char 0))
          (update this :current-char dec)

          :else this))

  (draw [this]
    (.setProjectionMatrix sprite-batch (.combined @c/camera))
    (.setProjectionMatrix shape-renderer (.combined @c/camera))
    (.begin sprite-batch)
    (c/draw-centered-text sprite-batch font "Game Over" 220)
    (.end sprite-batch)

    (when (gd/high-score? tentative-score)
      (.begin sprite-batch)
      (c/draw-centered-text sprite-batch font
                            (str "New High Score: " tentative-score) 180)
      (doseq [[c i] (map list
                         (map (partial letters) letter-indexes)
                         (range))]
        (.draw font sprite-batch (str c)
               (float (+ 230 (* 14 i)))
               (float 120)))
      (.end sprite-batch)

      (.begin shape-renderer ShapeRenderer$ShapeType/Line)
      (.line shape-renderer
             (+ 230 (* 14 current-char)) 100
             (+ 244 (* 14 current-char)) 100))
    (.end shape-renderer))

  (dispose [this]
    (.dispose sprite-batch)
    (.dispose shape-renderer)
    (.dispose title-font)
    (.dispose font)))


(defn make-game-over-state
  [tentative-score]
  (map->GameOverState {:tentative-score tentative-score}))
