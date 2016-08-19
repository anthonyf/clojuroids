(ns clojuroids.timer)

(defn make-timer
  [n]
  {:timer 0 :time n})

(defn timer-elapsed?
  [{:keys [timer time]}]
  (> timer time))

(defn update-timer
  [timer delta-time]
  (update timer :timer #(+ % delta-time)))

(defn reset-timer
  [timer]
  (assoc timer :timer 0))
