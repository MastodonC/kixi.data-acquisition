(ns kixi.data-acquisition.schema
  (:require [clojure.spec :as s]
            [clj-time.core :as t]
            [clj-time.format :as tf]))

(defn timestamp
  []
  (tf/unparse
   (tf/formatters :basic-date-time)
   (t/now)))

(defn timestamp?
  [s]
  (tf/parse
   (tf/formatters :basic-date-time)
   s))

(defn uuid?
  [s]
  (and (string? s)
       (re-find #"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
                s)))
