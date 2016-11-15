(ns kixi.data-acquisition.db.inmemory
  (:require [kixi.data-acquisition.db :refer :all :as db]
            [kixi.data-acquisition.request-to-share :as rts]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))

(defrecord DbInMemory []
  db/Database
  (create-table! [this table]
    (create-table! this table nil))
  (create-table! [this table index]
    (do
      (swap! (:data this)
             #(assoc % table
                     {:rows []
                      :index index}))
      nil))
  (insert! [this table row]
    (if-let [t (get @(:data this) table)]
      (if-not (and (:index t) (not (contains? row (:index t))))
        (do
          (swap! (:data this) #(update-in % [table :rows] (fn [x] (conj x row))))
          nil)
        (throw (Exception. (str "Row doesn't contain index: " (:index t)))))
      (throw (Exception. (str "Couldn't find table: " table)))))
  (insert! [this table row args]
    (insert! this table row nil))
  (select [this table what]
    (if what
      (mapv #(select-keys % what) (get-in @(:data this) [table :rows]))
      (vec (get-in @(:data this) [table :rows]))))
  (select-where [this table what where]
    (let [r (select this table what)]
      (vec (reduce (fn [a [k v]] (filter #(= (get % k) v) a)) r where))))
  (update! [this table what where]
    (throw (java.lang.UnsupportedOperationException.)))
  component/Lifecycle
  (start [component]
    (log/info "Starting Database - In Memory")
    ;; THIS IS USUALLY DONE BY MIGRATIONS
    (let [new-component (assoc component :data (atom {}))]
      (run! (partial db/create-table! new-component) rts/request-to-share-tables)
      new-component))
  (stop [component]
    (log/info "Stopping Database - In Memory")
    (dissoc component :data)))
