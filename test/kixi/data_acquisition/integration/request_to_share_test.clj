(ns kixi.data-acquisition.integration.request-to-share-test
  (:require [clojure.test :refer :all]
            [kixi.comms :as kc]
            [kixi.data-acquisition.db :as db]
            [kixi.data-acquisition.request-to-share :refer :all]
            [kixi.data-acquisition.integration.base :refer :all]))

(def system (atom nil))
(defn uuid [] (str (java.util.UUID/randomUUID)))

(use-fixtures :once (partial cycle-system-fixture system))

(deftest create-request-to-share-1-0-0-test
  (let [{:keys [comms db]} @system
        result-fn #(db/select db :request-to-share [:data-acquisition/request-id])
        req-id    (uuid)
        reqr-id   (uuid)
        sch-id    (uuid)
        reqe-name "foo"
        reqe-msg  "bar"]
    (kc/send-command! comms
                      :data-acquisition/create-request-to-share
                      "1.0.0"
                      {:data-acquisition/request-id     req-id
                       :data-acquisition/requester-id   reqr-id
                       :data-acquisition/schema-id      sch-id
                       :data-acquisition/requestee-name reqe-name
                       :data-acquisition/requestee-msg  reqe-msg})
    (wait-for-pred #(not-empty (result-fn)))
    (is (= req-id (:data-acquisition/request-id (first (result-fn)))))))
