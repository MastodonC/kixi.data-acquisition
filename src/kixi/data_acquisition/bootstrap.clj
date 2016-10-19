(ns kixi.data-acquisition.bootstrap
  (:require [kixi.data-acquisition.system]
            [com.stuartsierra.component :as component])
  (:gen-class))

(defn -main
  [& args]
  (let [config-profile (keyword (first args))
        system (kixi.data-acquisition.system/new-system config-profile)]
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. #(component/stop-system system)))
    (component/start-system system)
    (.. (Thread/currentThread) join)))
