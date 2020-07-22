(ns systems.bread.bread-datahike.core
  (:require
   [datahike.api :as d]
   [systems.bread.alpha.core :as bread]
   [systems.bread.alpha.datastore :as store]))


(extend-protocol store/TemporalDatastore
  datahike.db.DB
  (as-of [store instant]
    (d/as-of store instant))
  (history [store]
    (d/history store))
  (q [store query]
    (d/q query store))
  (pull [store query ident]
    (d/pull store query ident))
  (db-with [store tx]
    (d/db-with store tx))

  datahike.db.AsOfDB
  (q [store query]
    (d/q query store))

  datahike.db.HistoricalDB
  (q [store query]
    (d/q query store)))


(extend-protocol store/TransactionalDatastoreConnection
  clojure.lang.Atom
  (transact [conn tx]
    (d/transact conn tx)))


(def ^:private created-dbs (atom {}))

(defn connect [datahike-config]
  ;; TODO detect if db exists?
  (try
    (let [db-id (get-in datahike-config [:store :id])]
      (when (nil? (get @created-dbs db-id))
        (d/create-database (assoc datahike-config :initial-tx datahike-config))
        (swap! created-dbs assoc db-id {:created true})))
    (catch clojure.lang.ExceptionInfo _
      (println "db detected")))
  (try
    (d/connect datahike-config)
    (catch java.lang.IllegalArgumentException e
      (throw (ex-info (str "Exception connecting to datahike: " (.getMessage e))
                      {:exception e
                       :message (.getMessage e)
                       :config datahike-config})))))

(defn req->timepoint [{:keys [params] :as req}]
  (let [as-of-param (bread/config req :datastore/as-of-param)
        as-of (get params as-of-param)]
    (when as-of
      (try
        (.parse (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss z") as-of)
        (catch java.text.ParseException _e nil)))))

(defn req->datastore [req]
  (let [conn (bread/config req :datastore/connection)
        timepoint (bread/hook req :hook/datastore.req->timepoint)]
    (println timepoint)
    (if timepoint
      (store/as-of @conn timepoint)
      @conn)))

(defn datahike-plugin [config]
  (let [{:keys [as-of-param datahike]} config]
    (fn [app]
      (-> app
          (bread/set-config :datastore/connection (connect datahike))
          (bread/set-config :datastore/as-of-param (or as-of-param :as-of))
          (bread/add-hook :hook/datastore.req->timepoint req->timepoint)
          (bread/add-hook :hook/datastore req->datastore)))))


(comment
  (let [as-of (.parse (java.text.SimpleDateFormat. "yyyy-MM-dd") "2020-01-01")
        db (connect {:store {:backend :mem :id "qwerty"}})]
    (d/as-of @db as-of))

  (let [handler (-> {:plugins [(datahike-plugin {:as-of-param :timestamp})]}
                    (bread/app)
                    (bread/app->handler))]
    (handler {:params {:timestamp "2020-01-01"}})))