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
        result-fn #(db/select db :request-to-share [:kixi.data-acquisition.request-to-share/request-id])
        req-id    (uuid)
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
    (wait-for-pred #(not-empty (result-fn)))
    (is (= req-id (:kixi.data-acquisition.request-to-share/request-id (first (result-fn)))) (pr-str db))))
