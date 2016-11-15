(ns migrators.cassandra.201611141321-init
  (:use [joplin.cassandra.database])
  (:require [qbits.alia :as alia]
            [qbits.hayt :as hayt]))

(def initial-table
  {:request_id      :uuid
   :requester_id    :uuid
   :schema_id       :uuid
   :recipient_ids   (hayt/list-type :uuid)
   :destination_ids (hayt/list-type :uuid)
   :response_ids    (hayt/list-type :uuid)
   :message         :text
   :created_at      :timestamp})

(defn up
  [db]
  (let [conn (get-connection (:hosts db) (:keyspace db))]
    (alia/execute
     conn
     (hayt/create-table
      "data_requests"
      (hayt/column-definitions (merge initial-table {:primary-key [:request_id]}))))

    (alia/execute
     conn
     (hayt/create-table
      "data_requests_by_requester"
      (hayt/column-definitions (merge initial-table {:primary-key [:requester_id :request_id]}))))))

(defn down [db]
  (let [conn (get-connection (:hosts db) (:keyspace db))]
    (alia/execute conn (hayt/drop-table "data_requests"))
    (alia/execute conn (hayt/drop-table "data_requests_by_requester"))))
