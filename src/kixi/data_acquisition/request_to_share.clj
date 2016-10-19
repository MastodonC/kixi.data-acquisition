(ns kixi.data-acquisition.request-to-share
  (:require [kixi.data-acquisition.db :refer :all :as db]
            [kixi.comms :as kc]
            [kixi.comms.schema :as ks]
            [clojure.spec :as s]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema

(s/def :data-acquisition/request-id     ks/uuid?)
(s/def :data-acquisition/requester-id   ks/uuid?)
(s/def :data-acquisition/schema-id      ks/uuid?)
(s/def :data-acquisition/requestee-name string?)
(s/def :data-acquisition/requestee-msg  string?)

(s/def ::request-to-share-payload-1-0-0
  (s/keys :req [:data-acquisition/request-id
                :data-acquisition/requester-id
                :data-acquisition/schema-id
                :data-acquisition/requestee-name
                :data-acquisition/requestee-msg]))

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
  (log/debug "Got request to create a RTS")
  (if-let [error-event (error? ::request-to-share-payload-1-0-0
                               payload
                               :data-acquisition/request-to-share-failed
                               "1.0.0")]
    error-event
    (let [{:keys [data-acquisition/requester-id
                  data-acquisition/schema-id]} payload]
      ;; TODO Add checks
      ;; - Does a request with this requester + target + schema combo already exist?
      {:kixi.comms.event/key :data-acquisition/request-to-share-created
       :kixi.comms.event/version "1.0.0"
       :kixi.comms.event/payload payload}))) ;; pass the payload straight through

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Events

(defn request-to-share-created-1-0-0
  [db {:keys [kixi.comms.event/payload]}]
  (log/debug "Got RTS created event" payload)
  (let [{:keys [data-acquisition/requester-id
                data-acquisition/schema-id]} payload]
    (db/insert! db request-to-share-table payload)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Registers

(def command-register
  {:data-acquisition/create-request-to-share
   {:group :rts-create
    :versions {"1.0.0" create-request-to-share-1-0-0}}})

(def event-register
  {:data-acquisition/request-to-share-created
   {:group :rts-created
    :versions {"1.0.0" request-to-share-created-1-0-0}}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component

(defrecord RequestToShare [comms]
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
