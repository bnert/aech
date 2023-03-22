(ns aech.serde
  (:require
    [aech.protos.serde :refer [Serde]]
    [jsonista.core :as json]))

(def ^:dynamic *json*
  (reify Serde
    (serialize
      [_this input]
      (json/write-value input {}))
    (deserialize
      [_this input]
      (json/read-value input))

(
