(ns kixi.data-acquisition.integration.request-to-share-test
  (:require [clojure.test :refer :all]
            [kixi.comms :as kc]
            [kixi.data-acquisition.db :as db]
            [kixi.data-acquisition.request-to-share :refer :all]
            [kixi.data-acquisition.integration.base :refer :all]))

(def system (atom nil))

(use-fixtures :once (partial cycle-system-fixture system))

(deftest create-request-to-share-1-0-0-test
  (let [{:keys [comms request-to-share]} @system
        req-id    (uuid)
        req2-id   (uuid)
        reqr-id   (uuid)
        sch-id    (uuid)
        reqe-id   (uuid)
        dst-id    (uuid)
        reqe-msg  "bar"]
    ;; 1. Create a Request
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
    (wait-for-pred #(not-empty (fetch-by-id request-to-share (uuid req-id))))
    (is (= req-id (:kixi.data-acquisition.request-to-share/request-id (first (fetch-by-id request-to-share (uuid req-id))))))
    (is (= 1 (count (fetch-by-requester request-to-share (uuid reqr-id)))))
    (is (= req-id (:kixi.data-acquisition.request-to-share/request-id (first (fetch-by-requester request-to-share (uuid reqr-id))))))
    ;; 2. Create another Request
    (kc/send-command!
     comms
     :kixi.data-acquisition.request-to-share/create
     "1.0.0"
     {:kixi.data-acquisition.request-to-share/request-id       req2-id
      :kixi.data-acquisition.request-to-share/requester-id     reqr-id
      :kixi.data-acquisition.request-to-share/schema-id        sch-id
      :kixi.data-acquisition.request-to-share/recipient-ids    [reqe-id]
      :kixi.data-acquisition.request-to-share/destination-ids  [dst-id]
      :kixi.data-acquisition.request-to-share/message          reqe-msg})
    (wait-for-pred #(not-empty (fetch-by-id request-to-share (uuid req2-id))))
    (is (= req2-id (:kixi.data-acquisition.request-to-share/request-id (first (fetch-by-id request-to-share (uuid req2-id))))))
    (is (= 2 (count (fetch-by-requester request-to-share (uuid reqr-id)))))
    ;; 3. Submit Data to the first request
    ;; TODO
    ))
