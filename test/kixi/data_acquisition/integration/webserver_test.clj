(ns kixi.data-acquisition.integration.webserver-test
  (:require [clojure.test :refer :all]
            [kixi.data-acquisition.webserver :refer :all]
            [kixi.data-acquisition.integration.base :refer :all]
            [kixi.data-acquisition.unit.webserver-test :as wst]
            [org.httpkit.client :as http]
            [kixi.comms :as kc]))

(def system (atom nil))

(use-fixtures :once (partial cycle-system-fixture system))

(deftest create-request-test
  (let [{:keys [comms db]} @system
        req-id    (uuid)
        result-fn #(deref (http/get (wst/endpoint 60016 "request" "id" req-id)))
        reqr-id   (uuid)
        sch-id    (uuid)
        reqe-id   (uuid)
        dst-id    (uuid)
        reqe-msg  "bar"]
    (kc/send-command!
     comms
     :kixi.data-acquisition.request-to-share/create
     "1.0.0"
     {:kixi.data-acquisition.request-to-share/request-id       req-id
      :kixi.data-acquisition.request-to-share/requester-id     reqr-id
      :kixi.data-acquisition.request-to-share/schema-id        sch-id
      :kixi.data-acquisition.request-to-share/recipient-ids    [reqe-id]
      :kixi.data-acquisition.request-to-share/destination-ids  [dst-id]
      :kixi.data-acquisition.request-to-share/message          reqe-msg})
    (wait-for-pred #(= 200 (:status (result-fn))))
    (let [result (result-fn)
          body (-> result
                   :body
                   (slurp)
                   (transit-decode)
                   (first))]
      (is (= 200 (:status result)))
      (is (= req-id (:kixi.data-acquisition.request-to-share/request-id body)) (pr-str (-> result
                                                                                           :body
                                                                                           (slurp)))))
    (let [result @(http/get (wst/endpoint 60016 "request" "requester" reqr-id))
          body (-> result
                   :body
                   (slurp)
                   (transit-decode)
                   (first))]
      (is (= 200 (:status result)))
      (is (= req-id (:kixi.data-acquisition.request-to-share/request-id body))))))
