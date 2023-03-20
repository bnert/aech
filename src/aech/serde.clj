(ns aech.serde
  (:require
    [aech.protos.serde :refer [Serde]]
    [jsonista.core :as json]))

(def ^:dynamic *json*
  (reify Serde
    (serialize
      [_this input]
      (json/write-value input {}))
    (serialize
      [_this input opts]
      (json/write-value input opts))
    (deserialize
      [_this input]
      (json/read-value input))
    (deserialize
      [_this input opts]
      (json/read-value input opts))))

(extend-type nil Serde
  (serialize [_this] "")
  (deserialize [_this] nil))

