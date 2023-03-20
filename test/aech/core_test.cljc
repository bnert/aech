(ns aech.core-test
  (:require
    #?(:clj  [clojure.test :refer [deftest is testing]]
       :cljs [cljs.test :refer [async deftest is testing]])
    #?(:clj  [clojure.core.async :refer [<!! <! go]]
       :cljs [clojure.core.async :refer [take! <! go]])
    [aech.core :refer [fetch!]]))

(defn ismorph-async
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
    (ismorph-async
      (go
        (let [res (<! (fetch! "https://httpbin.org/get"))]
          (is (= 200 (:status res)))
          (is (= "OK" (:statusText res)))
          (is (true? (:ok res)))
          (is (map? (:body res)))
          (is (map? (:headers res))))))))

