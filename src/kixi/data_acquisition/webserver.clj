(ns kixi.data-acquisition.webserver
  (:gen-class)
  (:require [org.httpkit.server           :as httpkit]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [com.stuartsierra.component   :as component]
            [taoensso.timbre              :as log]
            [ring.middleware.cors         :refer [wrap-cors]]
            [compojure.core               :refer :all]
            [clojure.spec                 :as s]
            [cognitect.transit            :as tr]
            [kixi.data-acquisition.schema :as kds]
            [kixi.data-acquisition.request-to-share :as rts :refer [IRequestToShare]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(def transit-encoding-level :json-verbose) ;; DO NOT CHANGE
(defn transit-decode [s]
  (let [sbytes (.getBytes s)
        in (ByteArrayInputStream. sbytes)
        reader (tr/reader in transit-encoding-level)]
    (tr/read reader)))
(defn transit-encode [s]
  (let [out (ByteArrayOutputStream. 4096)
        writer (tr/writer out transit-encoding-level)]
    (tr/write writer s)
    (.toString out)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro when-valid
  [schema-value-pairs & body]
  (let [check-valid (fn [pair] (apply s/explain-data pair))]
    `(let [results# (->> ~schema-value-pairs
                         (partition 2)
                         (map ~check-valid)
                         (keep identity))]
       (if-not (empty? results#)
         {:status 400
          :body (pr-str results#)}
         (do
           ~@body)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn request-by-id
  [fnc request]
  (let [id (get-in request [:params :id])]
    (when-valid [kds/uuid? id]
      (if-let [rts (:request-to-share (::components request))]
        (if-let [request (fnc rts id)]
          {:status 200
           :body (transit-encode request)}
          {:status 404})
        {:status 424}))))

(def request-by-request-id
  (partial request-by-id #(rts/fetch-by-id %1 %2)))

(def request-by-requester-id
  (partial request-by-id #(rts/fetch-by-requester %1 %2)))

(defroutes app
  (context "/request" []
           (GET "/id/:id" req (request-by-request-id req))
           (GET "/requester/:id" req (request-by-requester-id req)))
  (GET "/health" [] (str "hello")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-log [handler]
  (fn [request]
    (log/info "REQUEST:" request)
    (handler request)))

(defn wrap-catch-exceptions [handler]
  (fn [request]
    (try (handler request)
         (catch Throwable t (log/error t)))))

(defn wrap-components
  "Assoc given components to the request."
  [handler components]
  (fn [req]
    (handler (assoc req ::components components))))

(defrecord WebServer [port]
  component/Lifecycle
  (start [this]
         (log/info (str "Server started at http://localhost:" port))
         (assoc this :http-kit (httpkit/run-server
                                (-> #'app
                                    (wrap-catch-exceptions)
                                    (wrap-components this)
                                    (wrap-log)
                                    (wrap-content-type "application/json")
                                    (wrap-cors :access-control-allow-origin [#".*"]
                                               :access-control-allow-methods [:get :post]))
                                {:port port})))
  (stop [this]
        (log/info "Stopping server")
        (if-let [http-kit (:http-kit this)]
          (http-kit :timeout 100))
    (dissoc this :http-kit)))
