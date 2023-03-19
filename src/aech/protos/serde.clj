(ns aech.protos.serde)

(defprotocol Serde
  (serialize
    [this input]
    [this input opts]
    "Serialize into an input stream/string")
  (deserialize
    [this input]
    [this input opts]
    "Deserialize from input stream/string into clojure data structures"))


(defprotocol FromJson
  (json [this] "Returns a promise which resolves to data from json string"))

(defprotocol FromText
   (text [this] "returns a promise which resolve to data a string"))

