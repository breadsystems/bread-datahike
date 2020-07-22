(ns systems.bread.bread-datahike.core-test
  (:require
   [datahike.api :as d]
   [systems.bread.alpha.core :as bread]
   [systems.bread.alpha.datastore :as store]
   [systems.bread.bread-datahike.core :as plugin]
   [clojure.test :refer [deftest is testing]]))


(deftest test-datahike-datastore
  (let [schema [{:db/ident :name
                 :db/valueType :db.type/string
                 :db/unique :db.unique/identity
                 :db/index true
                 :db/cardinality :db.cardinality/one}
                {:db/ident :age
                 :db/valueType :db.type/number
                 :db/cardinality :db.cardinality/one}]
        config {:store {:backend :mem :id "testdb"}
                :initial-tx schema}
        create-db (fn [conf]
                    (d/delete-database conf)
                    (d/create-database conf)
                    (d/connect conf))
        angela {:name "Angela" :age 76}
        bobby {:name "Bobby" :age 84}
        query-all '[:find ?n ?a
                    :where
                    [?e :name ?n]
                    [?e :age ?a]]
        init-db (fn []
                  (let [conn (create-db config)]
                    (store/transact conn [angela bobby])
                    conn))]

    (testing "it implements q (query)"
      (let [conn (init-db)]
        (is (= #{["Angela" 76] ["Bobby" 84]}
               (store/q @conn query-all)))))

    (testing "it implements pull"
      (let [conn (init-db)]
        (is (= {:name "Angela" :age 76 :db/id 3}
               (store/pull @conn '[*] [:name "Angela"])))))

    (testing "it supports transact"
      (let [conn (init-db)
            result (store/transact conn [{:db/id [:name "Angela"] :age 99}])]
        (is (instance? datahike.db.TxReport result))))

    (testing "it implements as-of"
      (let [conn (init-db)
            init-date (java.util.Date.)]
        ;; Happy birthday, Angela!
        (store/transact conn [{:db/id [:name "Angela"] :age 77}])
        (is (= #{["Angela" 77] ["Bobby" 84]}
               (store/q @conn query-all)))
        (is (= #{["Angela" 76] ["Bobby" 84]}
               (store/q (store/as-of @conn init-date) query-all)))))

    (testing "it implements history"
      (let [conn (init-db)
            query-ages '[:find ?a
                         :where
                         [?e :age ?a]
                         [?e :name "Angela"]]]
        (store/transact conn [{:db/id [:name "Angela"] :age 77}])
        (store/transact conn [{:db/id [:name "Angela"] :age 78}])
        (is (= #{[76] [77] [78]}
               (store/q (store/history @conn) query-ages)))))
    
    (testing "it implements with"
      (let [conn (init-db)
            db (store/db-with @conn [{:db/id [:name "Angela"] :age 77}])]
        (is (= {:name "Angela" :age 77 :db/id 3}
               (store/pull db '[*] [:name "Angela"])))))))


(deftest test-datahike-plugin
  (let [config {:datahike {:store {:backend :mem
                                   :id "plugin-db"}}}
        config->handler (fn [conf]
                          (-> {:plugins [(plugin/datahike-plugin conf)]}
                              (bread/app)
                              (bread/app->handler)))
        handle (fn [req]
                 ((config->handler config) req))]

    (testing "it configures as-of-param"
      (let [app (handle {})]
        (is (= :as-of (bread/config app :datastore/as-of-param)))))

    (testing "it honors custom as-of-param"
      (let [app (handle {})]
        (is (= :as-of (bread/config app :datastore/as-of-param)))))

    (testing "it configures db connection"
      (let [app (handle {})]
        (is (instance? clojure.lang.Atom (bread/config app :datastore/connection)))))

    (testing ":hook/datastore returns the present snapshot by default"
      (let [response ((config->handler config) {:url "/"})]
        (is (instance? datahike.db.DB (bread/hook response :hook/datastore)))))

    (testing ":hook/datastore.req->timepoint honors as-of param"
      (let [handler (config->handler config)
            response (handler {:url "/"
                               ;; pass a literal date here
                               :params {:as-of "2020-01-01 00:00:00 PDT"}})]
        (is (instance? datahike.db.AsOfDB (bread/hook response :hook/datastore)))))

    (testing ":hook/datastore.req->timepoint gracefully handles bad date strings"
      (let [handler (config->handler config)
            response (handler {:url "/"
                               :params {:as-of "nonsense date string"}})]
        (is (instance? datahike.db.DB (bread/hook response :hook/datastore)))))

    (testing "it honors a custom :hook/datastore.req->timepoint callback"
      (let [->timepoint (constantly (java.util.Date.))
            app (-> {:plugins [(plugin/datahike-plugin config)]}
                    (bread/app)
                    (bread/add-hook :hook/datastore.req->timepoint ->timepoint))
            handler (bread/app->handler app)
            response (handler {})]
        (is (instance? datahike.db.AsOfDB (bread/hook response :hook/datastore)))))))