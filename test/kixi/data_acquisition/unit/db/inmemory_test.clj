(ns kixi.data-acquisition.unit.db.inmemory-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [kixi.data-acquisition.db :as db]
            [kixi.data-acquisition.db.inmemory :refer :all]))

(def db (atom nil))

(defn create-db
  [all-tests]
  (let [system
        (component/start-system
         (component/system-map
          :db (map->DbInMemory {})))]
    (reset! db (:db system))
    (all-tests)
    (component/stop-system system)
    (reset! db nil)))

(use-fixtures :each create-db)

(deftest create-table-no-index
  (db/create-table! @db :test-table-a)
  (is (contains? (-> @db :data deref) :test-table-a)))

(deftest create-table-with-index
  (db/create-table! @db :test-table-b :foo)
  (is (contains? (-> @db :data deref) :test-table-b))
  (is (= :foo (get-in (-> @db :data deref) [:test-table-b :index]))))

(deftest insert-test
  (db/create-table! @db :test-table-a)
  (db/insert! @db :test-table-a {:foo 123})
  (is (= [{:foo 123}] (get-in (-> @db :data deref) [:test-table-a :rows])))
  (db/insert! @db :test-table-a {:foo 456})
  (is (= [{:foo 123} {:foo 456}] (get-in (-> @db :data deref) [:test-table-a :rows]))))

(deftest insert-test-bad-index
  (db/create-table! @db :test-table-a :foo)
  (is (thrown-with-msg? Exception #"Row doesn't contain index: :foo"
                        (db/insert! @db :test-table-a {:bar 123})))  )

(deftest select-test
  (db/create-table! @db :test-table-a)
  (db/insert! @db :test-table-a {:foo 123 :bar 456})
  (is (= [{:foo 123}] (db/select @db :test-table-a [:foo]))))

(deftest select-where-test
  (db/create-table! @db :test-table-a)
  (db/insert! @db :test-table-a {:foo 123 :bar "abc"})
  (db/insert! @db :test-table-a {:foo 456 :bar "def"})
  (db/insert! @db :test-table-a {:foo 789 :bar "ghi"})
  (is (= [{:bar "def"} (db/select-where @db :test-table-a [:bar] {:foo 456})])))
