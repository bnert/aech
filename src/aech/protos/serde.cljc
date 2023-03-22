(ns aech.protos.serde)

(defprotocol Serde
  (serialize
    [this input] "Serialize into an input stream/string")
  (deserialize
    [this input] "Deserialize from input stream/string into clojure data structures"))


(extend-type nil Serde
  (serialize [_this _input] nil)
  (deserialize [_this _input] nil))

