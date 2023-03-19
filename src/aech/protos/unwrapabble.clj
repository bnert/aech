(ns aech.protos.unwrapabble)

(defprotocol Unwrappable
  (unwrap [this]
    "Unwraps underlying value"))

