(ns kixi.data-acquisition.integration.base
  (:require [user :as repl]))

(defn cycle-system-fixture
  [a all-tests]
  (reset! a (repl/go))
  (all-tests)
  (repl/stop)
  (reset! a nil))

(defn wait-for-pred
  ([p]
   (wait-for-pred p 65))
  ([p tries]
   (wait-for-pred p tries 500))
  ([p tries ms]
   (loop [try tries]
     (when (and (pos? try) (not (p)))
       (Thread/sleep ms)
       (recur (dec try))))))
