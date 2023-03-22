(ns aech.spec
  (:require
    #?(:clj  clojure.core.async.impl.protocols
       :cljs cljs.core.async.impl.protocols)
    #?(:clj  [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s])))

#?(:clj
   (defn channel? [v]
     (satisfies?
       clojure.core.async.impl.protocols/Channel
       v)))

#?(:cljs
   (defn channel? [v]
     (satisfies?
       cljs.core.async.impl.protocols/Channel
       v)))

;; default is :get
(s/def :aech/method #{:get :post :put :patch :delete :head :options})

(s/def :aech/headers
  (s/map-of string? string?))

;; To be spec compliant with rfc (https://fetch.spec.whatwg.org/#bodies)
;; - [ ] A Stream (i.e. js/ReadableStream)
;; - [ ] A Source (i.e. byte seq, js/Blob, js/FormData)
;; - [ ] null
;;
;; Going w/ a string/map for now, since that is the simplest
(s/def :aech/body
  (s/or :str string?
        :map map?))

;; Per rfc: default is :no-cors
;; https://fetch.spec.whatwg.org/#requests
(s/def :aech/mode #{:cors :no-cors :same-origin})

(s/def :aech/credentials #{:omit :same-origin :include})

(s/def :aech/cache
  #{:default :no-store :reload :no-cache :force-cache :only-if-cached})

;; default :follow
(s/def :aech/redirect #{:follow :error :manual})

(s/def :aech/referrer string?)

(s/def :aech/referrer-policy
  #{:no-referrer
    :no-referrer-when-downgrade
    :same-origin
    :origin
    :strict-origin
    :origin-when-cross-origin
    :strict-origin-when-cross-origin
    :unsafe-url})

(s/def :aech/integrity string?)

(s/def :aech/keepalive boolean?)

(s/def :aech/signal channel?)

; default is :auto
(s/def :aech/priority #{:high :low :auto})

(s/def :aech/request
  (s/keys
    :req-un [:aech/method]
    :opt-un [:aech/headers
             :aech/body
             :aech/mode
             :aech/credentials
             :aech/cache
             :aech/redirect
             :aech/referrer
             :aech/referrerPolicy
             :aech/integrity
             :aech/keepalive
             :aech/signal
             :aech/priority]))

; -- response

(s/def :aech/status int?)

(s/def :aech/bodyUsed boolean?)

(s/def :aech/ok boolean?)

(s/def :aech/redirected boolean?)

(s/def :aech/status-text string?)

(s/def :aech/url string?)

(s/def :aech/type
  #{:basic :cors :error :opaque :opaqueredirect})

(s/def :aech/response
  (s/keys
    :req-un [:aech/status
             :aech/body
             :aech/bodyUsed
             :aech/headers
             :aech/ok
             :aech/redirected
             :aech/statusText
             :aech/url
             :aech/type]))

