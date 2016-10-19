(ns kixi.data-acquisition.db)

(defprotocol Database
  (create-table!
    [this table]
    [this table index])
  (insert!
    [this table row])
  (select
    [this table what])
  (select-where
    [this table what where]))
