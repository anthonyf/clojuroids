(ns clojuroids.flying-saucer
  (:require [clojuroids.space-object :as so]
            [clojuroids.common :as c]
            [clojuroids.jukebox :as j]
            [clojuroids.timer :as t]
            [clojuroids.bullet :as b])
  (:import [com.badlogic.gdx.math MathUtils]
           [com.badlogic.gdx.graphics.glutils
            ShapeRenderer ShapeRenderer$ShapeType]))

(defrecord FlyingSaucer
    [space-object
     type direction fire-timer
     path-timer-1 path-timer-2])


(def speed 70)

(defn score
  [flying-saucer]
  ((:type flying-saucer)
   {:large 200
    :small 1000}))

(def bullets-per-second 1)
(def path-time-1 2)
(def path-time-2 (+ path-time-1 2))
(def flying-saucer-color [1 1 1 1])

(def flying-saucer-shape
  {:large [[-10 0][-3 -5][3 -5]
           [10 0][3 5][-3 5]]
   :small [[-6 0][-2 -3][2 -3]
           [6 0][2 3][-2 3]]})

(defn make-flying-saucer-shape
  [[x y] type]
  (map (partial so/vector-add [x y])
       (type flying-saucer-shape)))

(defn make-flying-saucer
  [direction type]
  (let [[sw sh]            c/screen-size
        y                  (MathUtils/random sh)
        {:keys [pos dpos]} (if (= direction :left)
                             {:dpos [(- speed) 0]
                              :pos  [sw y]}
                             {:dpos [speed 0]
                              :pos  [0 y]})]
    (j/loop-sound (case type
                    :large :largesaucer
                    :small :smallsaucer))
    (map->FlyingSaucer {:type         type
                        :direction    direction
                        :space-object (so/make-space-object
                                       :pos pos :dpos dpos
                                       :shape (make-flying-saucer-shape pos type))
                        :fire-timer   (t/make-timer bullets-per-second)
                        :path-timer-1 (t/make-timer path-time-1)
                        :path-timer-2 (t/make-timer path-time-2)})))


(defn fire-bullet
  [state]
  (j/play-sound :saucershoot)
  (let [{:keys [player flying-saucer]} state
        {:keys [space-object type]} flying-saucer
        {[x y] :pos} space-object
        {player-so :space-object} player
        {[player-x player-y] :pos} player-so]
    (update state
            :enemy-bullets conj (b/make-bullet
                                 [x y]
                                 (case type
                                   :large (MathUtils/random (* 2 Math/PI))
                                   :small (MathUtils/atan2
                                           (- player-y y)
                                           (- player-x x)))))))

(defn update-flying-saucer
  [state delta-time]
  (as-> state state
    (if-not (-> state :player :hit?)
      (as-> state state
        (update-in state [:flying-saucer :fire-timer] t/update-timer delta-time)
        (if (t/timer-elapsed? (-> state
                                  :flying-saucer
                                  :fire-timer))
          (-> state
              (update-in [:flying-saucer :fire-timer] t/reset-timer)
              fire-bullet)
          state))
      state)
    (update-in state [:flying-saucer :path-timer-1] t/update-timer delta-time)
    (update-in state [:flying-saucer :path-timer-2] t/update-timer delta-time)
    (assoc-in state [:flying-saucer :space-object :dpos 1]
              (cond (t/timer-elapsed? (-> state :flying-saucer :path-timer-2))
                    0

                    (t/timer-elapsed? (-> state :flying-saucer :path-timer-1))
                    (- speed)

                    :else 0))

    (assoc-in state [:flying-saucer :space-object :shape]
              (let [{{:keys [type space-object]} :flying-saucer} state
                    {pos :pos} space-object]
                (make-flying-saucer-shape pos type)))

    (update-in state [:flying-saucer :space-object] so/move delta-time)

    ;; remove flying saucer once it goes off the screen
    (let [{:keys [direction type space-object]} (:flying-saucer state)
          {[x _] :pos} space-object
          [sw _] c/screen-size]
      (if (or (and (= direction :right)
                   (> x sw))
              (and (= direction :left)
                   (< x 0)))
        (do
          (j/stop-sound (case type
                          :large :largesaucer
                          :small :smallsaucer))
          (assoc state :flying-saucer nil))
        (update-in state [:flying-saucer :space-object] so/wrap)))))

(defn draw-flying-saucer
  [flying-saucer shape-renderer]
  (let [{space-object :space-object} flying-saucer
        {shape :shape} space-object
        [[x1 y1] _ _ [x2 y2] _ _] shape
        [r g b a] flying-saucer-color]
    (.setColor shape-renderer r g b a)
    (so/draw-space-object space-object shape-renderer flying-saucer-color)
    (.begin shape-renderer ShapeRenderer$ShapeType/Line)
    (.line shape-renderer x1 y1 x2 y2)
    (.end shape-renderer)))
