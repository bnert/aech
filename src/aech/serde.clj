(ns aech.serde
  (:require
    [aech.protos.serde :refer [Serde]]
    [clojure.data.xml :as xml]
    [jsonista.core]
    [hickory.core]
    [hickory.render])
  (:import
    [java.io InputStream]))

(defn slurp? [input-stream?]
  (if (instance? InputStream input-stream?)
    (slurp input-stream?)
    input-stream?))

(def ->hiccup (comp hickory.core/as-hiccup
                    hickory.core/parse
                    slurp?))

(def ^:dynamic *json*
  (reify Serde
    (serialize
      [_this input]
      (jsonista.core/write-value input {}))
    (serialize
      [_this input opts]
      (jsonista.core/write-value input opts))
    (deserialize
      [_this input]
      (jsonista.core/read-value input))
    (deserialize
      [_this input opts]
      (jsonista.core/read-value input opts))))

(def ^:dynamic *html*
  (reify Serde
    (serialize [_this input]
      (hickory.render/hiccup-to-html input))
    (serialize [_this input _opts]
      (hickory.render/hiccup-to-html input))
    (deserialize [_this input]
      (->hiccup input))
    (deserialize [_this input _opts]
      (->hiccup input))))

(def ^:dynamic *xml*
  (reify Serde
    (serialize [_this input]
      (xml/emit-str (if (vector? input)
                      (xml/sexp-as-element input)
                      input)))
    (serialize [_this input _opts]
      (xml/emit-str (if (vector? input)
                      (xml/sexp-as-element input)
                      input)))
    (deserialize [_this input]
      (xml/parse input))
    (deserialize [_this input _opts]
      (xml/parse input))))

(extend-type nil Serde
  (serialize [_this] "")
  (deserialize [_this] nil))

