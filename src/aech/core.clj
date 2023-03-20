(ns aech.core
  (:require
    [clojure.core.async :refer [chan
                                dropping-buffer
                                put!
                                take!
                                timeout]]
    [aech.decompression :refer [decompress-body]]
    [aech.http-status :refer [status->text]]
    [aech.protos.unwrappable :refer [unwrap]]
    [aech.protos.serde :refer [deserialize serialize]]
    [aech.signal :refer [abort-signal!]]
    [aech.serde :as serde]
    [clojure.string :as str])
  (:import
      [clojure.lang ExceptionInfo]
      [java.io InputStream]
      [java.net URI]
      [java.net.http HttpClient
                     HttpClient$Redirect
                     HttpClient$Version
                     HttpResponse
                     HttpResponse$BodyHandlers
                     HttpRequest
                     HttpRequest$Builder
                     HttpRequest$BodyPublisher
                     HttpRequest$BodyPublishers
                     HttpRequest]
      [java.util.concurrent CompletableFuture]
      [java.util.function Function]))

(defrecord Request
  [method
   headers
   body
   mode
   credentials
   cache
   integrity
   keepalive?
   priority
   redirect?
   referrer
   signal
   url])

(defrecord Response
  [body
   bodyUsed
   headers
   ok
   redirected
   status
   statusText
   kind
   url])


(defn java-list? [maybe-list]
  (instance? java.util.List maybe-list))

(def Request! map->Request)

(def Response! map->Response)

(defn ->BodyPublisher
  ^HttpRequest$BodyPublisher [r]
  (let [{:keys [body method]} r]
    (if (contains? #{:get :head} method)
      (HttpRequest$BodyPublishers/noBody)
      (if-not (string? body)
        (throw (ex-info "invalid body"
                        {:cause  :invalid-body
                         :reason "body should be a string"}))
        (HttpRequest$BodyPublishers/ofString body)))))


(defn map->headers
  ^HttpRequest$Builder [b header-map]
  (reduce-kv
    (fn [^HttpRequest$Builder b k v]
      (.header b
               (if (keyword? k) (name k) (str k))
               v))
    b
    header-map))

(defn Request->HttpRequest
  ^HttpRequest [r]
  (-> (HttpRequest/newBuilder)
      (.uri (URI. (:url r)))
      (map->headers (or (r :headers) {}))
      (.method (str/upper-case (name (:method r)))
               (->BodyPublisher r))
      (.build)))

(defn ->HttpClient [{:keys [redirect? http2?] :as _options}]
  (-> (HttpClient/newBuilder)
      (.followRedirects
        (case redirect?
          :always HttpClient$Redirect/ALWAYS
          :never HttpClient$Redirect/NEVER
          HttpClient$Redirect/NORMAL))
      (.version
        (if http2?
          HttpClient$Version/HTTP_2
          HttpClient$Version/HTTP_1_1))
      (.build)))

(defmulti deserialize-body
  (fn [resp]
    (when-let [ct (get-in resp [:headers "content-type"])]
      (cond
        (vector? ct) (first ct)
        (string? ct) (first (map str/trim (str/split ct #";")))
        :else        nil))))

(defmethod deserialize-body "application/json"
  [resp]
  (update resp :body #(deserialize serde/*json* %)))

(defmethod deserialize-body "text/plain"
  [resp]
  (update resp :body slurp))

(defmethod deserialize-body :default
  [resp]
  resp)

(defn ->Response' [^HttpResponse resp]
  (Response!
    {:body       (.body resp)
     :bodyUsed   false
     :headers    (reduce-kv
                   ; Header map is of type:
                   ;  Map<String, List<String>>, buuut just to be sure
                   (fn [acc k v]
                     (assoc acc k (if (java-list? v)
                                    (first v)
                                    v)))
                   {}
                   (.map (.headers resp)))
     :ok         (< (.statusCode resp) 399)
     :url        (-> resp (.uri) (.toString))
     :redirected (.isPresent (.previousResponse resp))
     :status     (.statusCode resp)
     :statusText (status->text (.statusCode resp))
     :kind       :basic})) ; or :cors

(def ^:dynamic *timeout* 10000) ; 10s, long but sufficient

(def ^:dynamic *auto-decompress* true)

(def ^:dynamic *auto-serialize* true)

(def ^:dynamic *auto-deserialize* true)

(def ^:dynamic *default-client*
  (->HttpClient {:redirect? :always
                 :http2?    false}))

(defn as-func [f]
  (reify java.util.function.Function
    (apply [_ arg]
      (f arg))))

(defn maybe-serialize-body [r]
  (if-not *auto-serialize*
    r
    (let [ct (or (get-in r [:headers "Content-Type"])
                 (get-in r [:headers "content-type"])
                 (get-in r [:headers :Content-Type])
                 (get-in r [:headers :content-type]))]
      (cond
        (= ct "application/json")
          (update r :body #(serialize serde/*json* %))
        :else
          r))))

(defn fetch*
  ([r]
   (fetch* *default-client* r))
  ([^HttpClient client req]
   (let [req    (maybe-serialize-body req)
         result (chan (dropping-buffer 1))
         signal (if (:signal req)
                  (:signal req)
                  (abort-signal! (timeout *timeout*)))
         fut    (.sendAsync client
                            ^HttpRequest (Request->HttpRequest req)
                            ^InputStream
                             (HttpResponse$BodyHandlers/ofInputStream))]
     (when signal
       (take! (unwrap signal)
              (fn [_]
                (.cancel ^CompletableFuture fut)
                (put! result {:ok false :error "aborted"}))))
     (-> fut
         (.thenApply
           (as-func
             (fn [res]
               (put! result
                     (cond-> (->Response' res)
                       *auto-decompress*  decompress-body
                       *auto-deserialize* deserialize-body)))))
         (.exceptionally
           (as-func
             #(put! result {:ok false :error %}))))
     result)))

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

(comment
  (require '[aech.core :as aech] :reload-all)

  (def r nil)
  (def r (aech/fetch! "https://httpbin.org/json"))
  (realized? r)
  (deref r)
  (identity r)

  (def r1 (aech/fetch! "https://httpbin.org/absolute-redirect/3"))
  (realized? r1)
  (deref r1)
 )
