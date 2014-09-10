(ns mea.core
  (:require [mea.config :as config]
            [datomic.api :as d])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn load-config [file]
  (with-open [^java.io.Reader reader (clojure.java.io/reader file)]
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props] [(keyword k) (read-string v)])))))

(defn load-txs [file]
  (clojure.edn/read-string (slurp file)))

(defn get-db [] (d/db conn))

(defn setup-db [db-uri schema-txs]
  (do
    (d/create-database db-uri)
    (d/transact (d/connect db-uri) schema-txs)))

(def datomic-config (config/load-config "config/datomic.properties"))
(def db-uri (get datomic-config :uri))

;; setup && seed database
(setup-db (load-txs "db/schema.edn"))
(d/transact (d/connect db-uri) (load-txs "db/seed.edn"))

;; our database connection
(def conn (d/connect db-uri))

(defn map->txs
  "Converts a namespace (keyword) and a map into a vector of vector assertions.
   This is used internally by entity constructors e.g. `create-participant'.

   e.g. (map->txs :db/add :participant {:first_name \"Peter\" :last_name \"Parker\"})"
  [tx ns m]
  (let [id (d/tempid :db.part/user)
        ks (keys m)
        make-tx (fn [k] [tx id (keyword (name ns) (name k)) (get m k)])]
    (vec (map make-tx ks))))

(defn create-participant
  "Creates participant entity from a map, returns a vector of the form:

       [db uuid]

   where `db' is the updated database and `uuid' is the participant's UUID."
  [conn proto]
  (let [id (d/squuid)
        tx (d/transact conn
                       (map->txs :db/add
                                 :participant
                                 (into {:participant_id id} proto)))]
    [(d/db conn) id]))

(defn get-participant
  "Find a participant by it's UUID, returns a dynamic map
   of the given participant's attributes"
  [db uuid]
  (map (fn [eid] (d/entity db eid))
       (first (d/q '[:find ?p :where [?p :participant/participant_id uuid]]))))
