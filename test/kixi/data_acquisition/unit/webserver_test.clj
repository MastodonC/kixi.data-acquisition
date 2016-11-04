(ns kixi.data-acquisition.unit.webserver-test
  (:require [clojure.test :refer :all]
            [kixi.data-acquisition.webserver :refer :all]
            [com.stuartsierra.component   :as component]
            [org.httpkit.client :as http]))

(def port 54321)

(defn cycle-webserver
  [all-tests]
  (let [server (map->WebServer {:port port})
        c (component/start server)]
    (all-tests)
    (component/stop c)))

(defn endpoint
  [port & path]
  (str "http://localhost:" port "/" (clojure.string/join "/" path)))

(use-fixtures :each cycle-webserver)

(deftest health-test
  (is (= "hello" (:body @(http/get (endpoint port "health"))))))

(deftest request-by-id-test
  (is (= 400 (:status @(http/get (endpoint port "request" "id" 12345)))))
  (is (= 424 (:status @(http/get (endpoint port "request" "id" (str (java.util.UUID/randomUUID))))))))

(deftest request-by-requester-test
  (is (= 400 (:status @(http/get (endpoint port "request" "requester" 12345)))))
  (is (= 424 (:status @(http/get (endpoint port "request" "requester" (str (java.util.UUID/randomUUID))))))))

(deftest when-valid-test
  (is (= {:foo 123} (when-valid [int? 1] {:foo 123})))
  (is (= {:foo 123} (when-valid [int? 1] {:bar 567} {:foo 123})))
  (is (= {:status 400} (dissoc (when-valid [string? 1] {:foo 123}) :body))))
