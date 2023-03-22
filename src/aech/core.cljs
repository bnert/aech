(ns aech.core
  (:require
    aech.spec 
    [aech.protos.serde :refer [deserialize]]
    [aech.serde :as serde]
    [cljs.spec.alpha :as s]
    [clojure.core.async :refer [go take!]]
    [clojure.core.async.interop :refer-macros [<p!]]
    [clojure.string :as str]))

(def ^:dynamic *timeout* 10000)

(def ^:dynamic *auto-serialize* true)

(def ^:dynamic *auto-deserialize* true)

(def ^:dynamic *json-content-types* #{"application/json"})

(def ^:dynamic *text-content-types* #{"text/html" "text/plain"})

(def ^:dynamic *json* serde/*json*)

(def ^:dynamic *auto-decode-responses*
  #{:json :text})

(defn error->response-map [e]
  {:status     0
   :ok         false
   :body       (.toString ^js e)
   :bodyUsed   true
   :redirected false
   :statusText "client error or network error"
   :url        ""
   :type       :error})

(defn json? [response']
  (let [decode?      (contains? *auto-decode-responses* :json)
        content-type (get-in response' [:headers "content-type"] "")]
    (and decode?
        (contains? *json-content-types* content-type))))

(defn text? [response']
  (let [decode?      (contains? *auto-decode-responses* :text)
        content-type (get-in response' [:headers "content-type"] "")]
    (and decode?
         (contains? *text-content-types* content-type))))

(defn body<-json [response' ^js response]
  (-> (.json response)
      (.then
        (fn [json-body]
          (-> response'
              (assoc :body (deserialize *json* json-body))
              (assoc :bodyUsed true))))
      (.catch
        error->response-map)))

(defn body<-text [response' ^js response]
  (-> (.text response)
      (.then
        (fn [t]
          (assoc response' :body t)))
      (.catch error->response-map)))

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
    (.forEach ^js h
              (fn [v k]
                (swap! result assoc (str/lower-case k) v)))
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


(def ^:dynamic *default-client* ^js js/fetch)

; out of the gate, only going to support text/json payloads
(defn fetch*
  [req]
  {:pre [(s/valid? :aech/request req)]}
  (let [signal?          (get req :signal)
        abort-controller ^js (js/AbortController.)]
    (go
      (when signal?
        (take! signal?
               (fn [_]
                 (.abort abort-controller))))
      (try
        (let [response
              (<p! (*default-client*
                     (req :url)
                     (clj->js
                       (-> (dissoc req :url)
                           (assoc :signal (.-signal abort-controller))))))
              response' (Response->map response)]
          (<p!
            (cond
              (json? response')
                (body<-json response' response)
              (text? response')
                (body<-text response' response)
              :else (.resolve js/Promise response))))
          (catch js/Error e
            {:status     0
             :ok         false
             :body       (.toString ^js e)
             :bodyUsed   true
             :redirected false
             :statusText "client error or network error"
             :url        (req :url)
             :type       :error})))))

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

