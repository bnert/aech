(ns aech.serde
  (:require
    [aech.protos.serde :refer [Serde]]))

(def ^:dynamic *json*
  (reify Serde
    (serialize
      [_this input]
        (.stringify js/JSON (clj->js input)))
    (deserialize
      [_this input]
      (let [input (if-not (string? input)
                    input
                    (.parse js/JSON input))]
        (js->clj input)))))


