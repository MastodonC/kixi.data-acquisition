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

(deftest db->clj-test
  (let [payload {:request-id (java.util.UUID/randomUUID)
                 :requester-id (java.util.UUID/randomUUID)
                 :schema-id (java.util.UUID/randomUUID)
                 :recipient-ids [(java.util.UUID/randomUUID) (java.util.UUID/randomUUID) (java.util.UUID/randomUUID)]
                 :destination-ids [(java.util.UUID/randomUUID) (java.util.UUID/randomUUID) (java.util.UUID/randomUUID)]}
        fixed (db->clj payload)]
    (is (= java.lang.String (type (::rts/request-id fixed))))
    (is (= java.lang.String (type (::rts/requester-id fixed))))
    (is (= java.lang.String (type (::rts/schema-id fixed))))
    (is (= clojure.lang.PersistentVector (type (::rts/recipient-ids fixed))))
    (is (= java.lang.String (type (first (::rts/recipient-ids fixed)))))
    (is (= clojure.lang.PersistentVector (type (::rts/destination-ids fixed))))
    (is (= java.lang.String (type (first (::rts/destination-ids fixed)))))))
