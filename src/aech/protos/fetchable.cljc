(ns aech.protos.fetchable)

(defprotocol Fetchable
  (fetch'
    [this options]
    [this client options]))

