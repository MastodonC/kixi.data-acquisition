(ns kixi.data-acquisition.system
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [com.stuartsierra.component :as component]
            ;;
            [kixi.comms.components.kafka :as kafka]
            [kixi.data-acquisition
             [logstash :as logstash]
             [metrics :as metrics]]
            [kixi.data-acquisition.db.inmemory :as db-inmemory]
            [kixi.data-acquisition.db.cassandra :as db-cassandra]
            [kixi.data-acquisition.datastore.inmemory :as ds-inmemory]
            [kixi.data-acquisition.request-to-share :as rts]
            [kixi.data-acquisition.webserver :as web]))

(defmethod aero/reader 'rand-uuid
  [{:keys [profile] :as opts} tag value]
  (str (java.util.UUID/randomUUID)))

(defn get-config
  "Read EDN config, with the given profile. See Aero docs at
  https://github.com/juxt/aero for details."
  [profile]
  (aero/read-config (io/resource "config.edn") {:profile profile}))

(defn new-system
  [profile]
  (let [config (get-config profile)]

    (if (= profile :production)
      (timbre/merge-config! (assoc (:log config) :output-fn logstash/output-fn))
      (timbre/merge-config! (:log config)))

    (component/system-map
     :db      (case (first (keys (:db config)))
                :inmemory (db-inmemory/map->DbInMemory {})
                :cassandra (db-cassandra/map->DbCassandra (merge {:profile profile}
                                                                 (get-in config [:db :cassandra]))))
     :datastore (case (first (keys (:datastore config)))
                  :inmemory (ds-inmemory/map->DatastoreInMemory {}))
     :comms (kafka/map->Kafka (:comms config))
     :metrics (metrics/map->Metrics (:metrics config))
     ;;
     :request-to-share (component/using
                        (rts/map->RequestToShare {})
                        [:comms :db])
     :webserver    (component/using
                    (web/map->WebServer (:webserver config))
                    [:request-to-share]))))
