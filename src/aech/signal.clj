(ns aech.signal
  (:require
    [aech.protos.unwrappable :refer [Unwrappable]]
    [clojure.core.async :refer [chan dropping-buffer put!]]))

(defprotocol Abortable
  (abort [this] "Signals to dependents to abort"))

(defrecord AbortSignal [signal]
  Abortable
  (abort [_this]
    (when signal
      (put! signal nil)))

  Unwrappable
  (unwrap [_this]
    signal))

(defn abort-signal!
  ([]
   (abort-signal! (chan (dropping-buffer 1))))
  ([signal]
   (map->AbortSignal {:signal signal})))

