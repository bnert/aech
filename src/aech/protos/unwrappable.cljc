(ns aech.protos.unwrappable)

(defprotocol Unwrappable
  (unwrap [this]
    "Unwraps underlying value"))

