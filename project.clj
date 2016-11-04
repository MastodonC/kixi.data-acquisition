(def metrics-version "2.7.0")
(def slf4j-version "1.7.21")
(defproject kixi/kixi.data-acquisition "0.1.0-SNAPSHOT"
  :description "Get data into the Kixi system"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/data.codec "0.1.0"]
                 [ring/ring-core "1.5.0"]
                 [aero "1.0.0"]
                 [clj-time "0.12.0"]
                 [clj-http "2.2.0"]
                 [com.izettle/dropwizard-metrics-influxdb "1.1.6" :exclusions [ch.qos.logback/logback-classic]]
                 [com.fzakaria/slf4j-timbre "0.3.2"]
                 [com.stuartsierra/component "0.3.1"]
                 [com.taoensso/timbre "4.7.0"]
                 [metrics-clojure ~metrics-version]
                 [metrics-clojure-jvm ~metrics-version]
                 [metrics-clojure-ring ~metrics-version]
                 [org.clojure/core.async "0.2.391"]
                 [org.clojure/tools.analyzer "0.6.9"]
                 [org.slf4j/log4j-over-slf4j ~slf4j-version]
                 [org.slf4j/jul-to-slf4j ~slf4j-version]
                 [org.slf4j/jcl-over-slf4j ~slf4j-version]
                 [kixi/kixi.comms "0.1.16"]
                 [http-kit "2.1.19"]
                 [ring-cors "0.1.8"]
                 [compojure "1.5.1"]
                 [com.cognitect/transit-clj "0.8.290"]]

  :main ^:skip-aot kixi.data-acquisition
  :target-path "target/%s"
  :profiles {:uberjar {:aot [kixi.data-acquisition.bootstrap]
                       :uberjar-name "kixi.data-acquisition-standalone.jar"}
             :dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :repl-options {:init-ns user}}}
  :test-selectors {:default (complement :integration)
                   :integration :integration}
  :global-vars {*warn-on-reflection* true
                *assert* false})
