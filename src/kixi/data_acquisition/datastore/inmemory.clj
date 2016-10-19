(ns kixi.data-acquisition.datastore.inmemory
  (:require [kixi.data-acquisition.datastore :refer :all :as ds]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))

(defrecord DatastoreInMemory []
  ds/Datastore
  (get-data
      [this])
  component/Lifecycle
  (start [component]
    (log/info "Starting Datastore - In Memory")
    component)
  (stop [component]
    (log/info "Stopping Datastore - In Memory")
    component))
