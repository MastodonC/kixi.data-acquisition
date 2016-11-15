(ns kixi.data-acquisition.db.cassandra
  (:require [com.stuartsierra.component :as component]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [clj-time.format :as tf]
            [clj-time.core :as t]
            [clojure.java.io :as io]
            [joplin.repl :as jrepl :refer [migrate load-config]]
            [taoensso.timbre :as log]
            [kixi.data-acquisition.db :as db]))

(defn db-now
  "getting timestamp of now but in db format"
  []
  (tf/unparse (tf/formatters :date-time) (t/now)))

(defn replacer
  "Calls  replacement function on different types"
  [rfn x]
  (condp = (type x)
    clojure.lang.Keyword (-> x name rfn keyword)
    clojure.lang.MapEntry (update x 0 (partial replacer rfn))
    clojure.lang.PersistentArrayMap (map (partial replacer rfn) x)
    java.lang.String (rfn x)))

(defn underscore->hyphen
  "Converts underscores to hyphens"
  [x]
  (replacer #(clojure.string/replace % #"_" "-") x))

(defn hyphen->underscore
  "Convers hyphens to underscores"
  [x]
  (replacer #(clojure.string/replace % #"-" "_") x))

(defn exec
  [this x]
  (if-let [conn (get this :session)]
    (try
      (log/debug "Executing" (hayt/->raw x))
      (alia/execute conn x)
      (catch Exception e (log/error "Failed to execute database command:" (str e))))
    (log/error "Unable to execute Cassandra comment - no connection")))

(defn create-keyspace!
  [hosts keyspace replication-strategy]
  (alia/execute
   (alia/connect (alia/cluster {:contact-points hosts}))
   (hayt/create-keyspace keyspace
                         (hayt/if-exists false)
                         (hayt/with {:replication
                                     replication-strategy}))))

(defrecord DbCassandra [hosts keyspace replication-strategy profile]
  db/Database
  (create-table! [this table columns]
    (exec this (hayt/create-table table (hayt/column-definitions columns))))
  (insert! [this table row {:keys [using]}]
    (cond
      using (exec this (hayt/insert table (hayt/values row) (apply hayt/using using)))
      :else (exec this (hayt/insert table (hayt/values row)))))
  (insert! [this table row]
    (db/insert!
     this
     (hyphen->underscore table)
     (into {} (map hyphen->underscore row)) {}))
  (select [this table what]
    (let [table (hyphen->underscore table)
          result (if what
                   (exec this (hayt/select table (apply hayt/columns
                                                        (map hyphen->underscore what))))
                   (exec this (hayt/select table)))
          reformatted (map underscore->hyphen result)]
      (if (coll? result)
        (map (partial into {}) reformatted)
        reformatted)))
  (select-where [this table what where]
    (let [table (hyphen->underscore table)
          result (if what
                   (exec this (hayt/select table (apply hayt/columns
                                                        (map hyphen->underscore what))
                                           (hayt/where (into {} (hyphen->underscore where)))))
                   (exec this (hayt/select table
                                           (hayt/where (into {} (hyphen->underscore where))))))
          reformatted (map underscore->hyphen result)]
      (if (coll? result)
        (map (partial into {}) reformatted)
        reformatted)))
  (update! [this table what where]
    (exec this (hayt/update table (hayt/set-columns
                                   (into {} (underscore->hyphen what)))
                            (hayt/where where))))

  component/Lifecycle
  (start [component]
    (log/info "Bootstrapping Cassandra...")
    (log/info "Keyspace:" hosts keyspace replication-strategy)
    (create-keyspace! hosts keyspace replication-strategy)
    (log/info "Keyspace created")
    (let [joplin-config (jrepl/load-config (io/resource "joplin.edn"))]
      (log/info "About to migrate")
      (->> profile
           (migrate joplin-config)
           (with-out-str)
           (clojure.string/split-lines)
           (run! #(log/info "> JOPLIN:" %))))
    (log/info "Migrated")
    (log/info ";; Starting session")
    (assoc component
           :session
           (alia/connect
            (alia/cluster {:contact-points hosts}) keyspace)))

  (stop [component]
    (log/info ";; Stopping session")
    (when-let [session (:session component)]
      (alia/shutdown session)
      component)))
