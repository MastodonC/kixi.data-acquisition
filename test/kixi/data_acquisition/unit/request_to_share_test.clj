(ns kixi.data-acquisition.unit.request-to-share-test
  (:require [clojure.test :refer :all]
            [kixi.data-acquisition.integration.base :refer :all]
            [kixi.data-acquisition.request-to-share :as rts :refer :all]))

(deftest clj->db-test
  (let [payload {::rts/request-id (uuid)
                 ::rts/requester-id (uuid)
                 ::rts/schema-id (uuid)
                 ::rts/recipient-ids [(uuid) (uuid) (uuid)]
                 ::rts/destination-ids [(uuid) (uuid) (uuid)]}
        fixed (clj->db payload)]
    (is (= java.util.UUID (type (::rts/request-id fixed))))
    (is (= java.util.UUID (type (::rts/requester-id fixed))))
    (is (= java.util.UUID (type (::rts/schema-id fixed))))
    (is (= clojure.lang.PersistentVector (type (::rts/recipient-ids fixed))))
    (is (= java.util.UUID (type (first (::rts/recipient-ids fixed)))))
    (is (= clojure.lang.PersistentVector (type (::rts/destination-ids fixed))))
    (is (= java.util.UUID (type (first (::rts/destination-ids fixed)))))))
