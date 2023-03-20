(ns aech.core
  (:require
    [clojure.core.async :refer [go
                                chan
                                dropping-buffer
                                put!
                                timeout]]
    [clojure.core.async.interop :refer-macros [<p!]]))

(def ^:dynamic *timeout* 10000)

(def ^:dynamic *auto-serialize* true)

(def ^:dynamic *auto-deserialize* true)

(defn maybe-serialize-body [r]
  (if-not *auto-serialize*
    r
    (let [ct (or (get-in r [:headers "Content-Type"])
                 (get-in r [:headers "content-type"])
                 (get-in r [:headers :Content-Type])
                 (get-in r [:headers :content-type]))]
      (cond
        (= ct "application/json")
          (update r :body (comp (.-stringify js/JSON) clj->js))
        :else
          r))))

(defn Headers->map [^js/Headers h]
  (let [result (atom {})] ;lol not good
    (.forEach h
              (fn [v k]
                (swap! result assoc k v)))
    @result))

(defn Response->map [^js/Response r]
  {:body       (.-body r)
   :bodyUsed   (.-bodyUsed r)
   :headers    (Headers->map (.-headers r))
   :ok         (.-ok r)
   :redirected (.-redirected r)
   :status     (.-status r)
   :statusText (.-statusText r)
   :kind       (.-type r)
   :url        (.-url r)})

; out of the gate, only going to support text/json payloads
(defn fetch* [req]
  (let [req (maybe-serialize-body req)]
    (go
      (try
        (let [resp (<p! (js/fetch (:url req)
                                  (clj->js (dissoc req :url))))
              resp' (Response->map resp)]
          (if *auto-deserialize*
            (case (get (resp' :headers) "content-type")
              "application/json"
                (let [j (<p! (.json resp))
                      j (js->clj j)]
                  (assoc (Response->map resp) :body j))
              (let [t (<p! (.text resp))]
                (assoc (Response->map resp) :body t)))
            resp))
        (catch js/Error error
          {:ok false :error error})))))

(defn fetch!
  "Fetches remote resource for url.
  
  `resource` string - resource uri
  `options`  optional<string> - optional map of:
  - `:method`: kw
  - `:headers`: map
  - `:body`: any?. Not set on GET or HEAD
  - `:mode`: one of:
  - - :cors
  - - :no-cors
  - - :same-origin
  - `:credentials`
  - - :omit
  - - :same-origin
  - - :include
  - `:cache`
  - - :default, :no-store, :reload, :no-cache, :force-cache, :only-if-cached
  - `redirect`
  - - :follow, :error, :manual
  - `:referrer`
  - `:integrity`
  - `:keepalive`
  - `:signal`
  - - AbortController (how to do on JVM?)
  - `:priority`
  - - :high, :low, :auto

  Returns: Promise<ResponseMap>
  "
  ([resource]
   (fetch! resource {:method :get}))
  ([resource options]
   (fetch* (into options {:url resource}))))

