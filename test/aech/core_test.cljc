(ns aech.core-test
  (:require
    #?(:clj  [clojure.test :refer [deftest is testing]]
       :cljs [cljs.test :refer [async deftest is testing]])
    #?(:clj  [clojure.core.async :refer [<!! <! go]]
       :cljs [clojure.core.async :refer [take! <! go]])
    #?(:clj  [jsonista.core :as json])
    [aech.core :refer [fetch!]]
    [aech.protos.serde :refer [Serde]]))

(defn isomorph-async
  "Asynchronous test awaiting ch to produce a value or close."
  [ch]
  #?(:clj  (<!! ch)
     :cljs (async done
                  (take! ch (fn [_] (done))))))

(deftest sanity
  (testing "stanity"
    (is (true? true))))

(deftest httbin-get
  (testing "httbin get request"
    (isomorph-async
      (go
        (let [res (<! (fetch! "https://httpbin.org/get"))]
          (is (= 200 (:status res)))
          (is (= "OK" (:statusText res)))
          (is (true? (:ok res)))
          (is (map? (:body res)))
          (is (map? (:headers res))))))))

#?(:clj  (def json*
           (reify Serde
             (serialize [_this _input] nil)
             (deserialize [_this input]
               (json/read-value input json/keyword-keys-object-mapper))))
   :cljs (def json*
           (reify Serde
             (serialize [_this _input] nil)
             (deserialize [_this input]
               (js->clj input :keywordize-keys true)))))
(deftest httpbin-get-with-custom-json-decoder
  (testing "httbin get request w/ custom json decoder"
    (isomorph-async
      (go
        (binding [aech.core/*json* json*]
          (let [res (<! (fetch! "https://httpbin.org/get"))]
            (is (= 200 (:status res)))
            (is (= "OK" (:statusText res)))
            (is (true? (:ok res)))
            (is (map? (:body res)))
            (is (every? keyword? (keys (:body res))))
            (is (map? (:headers res)))))))))

