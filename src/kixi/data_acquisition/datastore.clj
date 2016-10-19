(ns kixi.data-acquisition.datastore)

(defprotocol Datastore
  (get-data
    [this]))
