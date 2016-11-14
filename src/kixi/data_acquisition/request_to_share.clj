(ns kixi.data-acquisition.request-to-share
  (:require [kixi.data-acquisition.db :as db]
            [kixi.comms :as kc]
            [kixi.comms.time :as kt]
            [kixi.comms.schema :as ks]
            [clojure.spec :as s]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema

(s/def ::request-id       ks/uuid?)
(s/def ::requester-id     ks/uuid?)
(s/def ::schema-id        ks/uuid?)
(s/def ::recipient-ids    (s/+ ks/uuid?))
(s/def ::destination-ids  (s/+ ks/uuid?))
(s/def ::message          string?)
(s/def ::created-at       kt/timestamp?)

(s/def ::request-to-share-payload-1-0-0
  (s/keys :req [::request-id
                ::requester-id
                ::schema-id
                ::recipient-ids
                ::destination-ids
                ::message]
          :opt [::created-at]))

(def request-to-share-table :request-to-share)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Functions

(defn error?
  [schema p k v]
  (when-let [data (s/explain-data schema p)]
    {:kixi.comms.event/payload data
     :kixi.comms.event/version v
     :kixi.comms.event/key k}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Commands

(defn create-request-to-share-1-0-0
  [db {:keys [kixi.comms.command/payload]}]
  (log/info "Got request to create a RTS")
  (if-let [error-event (error? ::request-to-share-payload-1-0-0
                               payload
                               :kixi.data-acquisition.request-to-share/create-failed
                               "1.0.0")]
    (do
      (log/error "Command failed :kixi.data-acquisition.request-to-share/create:" error-event)
      error-event)
    (let [{:keys [kixi.data-acquisition.request-to-share/requester-id
                  kixi.data-acquisition.request-to-share/schema-id]} payload]
      ;; TODO Add checks
      ;; - Does a request with this requester + target + schema combo already exist?
      {:kixi.comms.event/key :kixi.data-acquisition.request-to-share/created-successfully
       :kixi.comms.event/version "1.0.0"
       :kixi.comms.event/payload (assoc payload ::created-at (kt/timestamp))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Events

(defn request-to-share-created-1-0-0
  [db {:keys [kixi.comms.event/payload]}]
  (log/info "Got RTS created event" payload)
  (let [{:keys [kixi.data-acquisition.request-to-share/requester-id
                kixi.data-acquisition.request-to-share/schema-id]} payload]
    (db/insert! db request-to-share-table payload)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Registers

(def command-register
  {:kixi.data-acquisition.request-to-share/create
   {:group :rts-create
    :versions {"1.0.0" create-request-to-share-1-0-0}}})

(def event-register
  {:kixi.data-acquisition.request-to-share/created-successfully
   {:group :rts-created
    :versions {"1.0.0" request-to-share-created-1-0-0}}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Protocol

(defprotocol IRequestToShare
  (fetch-by-id [this id])
  (fetch-by-requester [this id]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component

(defrecord RequestToShare [comms db]
  IRequestToShare
  (fetch-by-id [this id]
    (not-empty
     (db/select-where db :request-to-share nil {:kixi.data-acquisition.request-to-share/request-id id})))
  (fetch-by-requester [this id]
    (not-empty
     (db/select-where db :request-to-share nil {:kixi.data-acquisition.request-to-share/requester-id id})))
  component/Lifecycle
  (start [{:keys [db] :as component}]
    (log/info "Starting Request-to-Share component")
    ;; commands
    (run! (fn [[command-key {:keys [group versions]}]]
            (run! (fn [[command-version command-handler]]
                    (log/debug "Adding command handler for" command-key command-version)
                    (kc/attach-command-handler!
                     comms group command-key command-version (partial command-handler db)))
                  versions))
          command-register)
    ;; event
    (run! (fn [[event-key {:keys [group versions]}]]
            (run! (fn [[event-version event-handler]]
                    (log/debug "Adding event handler for" event-key event-version)
                    (kc/attach-event-handler!
                     comms group event-key event-version (partial event-handler db)))
                  versions))
          event-register)
    component)
  (stop [component]
    (log/info "Stopping Request-to-Share component")
    component))
