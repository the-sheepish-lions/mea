(ns mea.core
  (:require [datomic.api :as d]
            [clojure.java.io :as io])
  (:import datomic.Util)
  (:gen-class))

(set! *warn-on-reflection* true)

(defn load-config [file]
  (with-open [^java.io.Reader reader (io/reader file)]
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props] [(keyword k) (read-string v)])))))

;; read-all and transact-all are taken from here:
;; https://github.com/Datomic/day-of-datomic/blob/053b3bd983d165b8fa7c0c039712fb1cb75eddf3/src/datomic/samples/io.clj

(defn read-all
  "Read all forms in f, where f is any resource that can
   be opened by io/reader"
  [f]
  (Util/readAll (io/reader f)))

(defn transact-all
  "Load and run all transactions from f, where f is any
   resource that can be opened by io/reader."
  [conn f]
  (doseq [txd (read-all f)]
    (d/transact conn txd))
  :done)

(defn setup-db [db-uri schema-txs]
  (do
    (d/create-database db-uri)
    (d/transact (d/connect db-uri) schema-txs)))

(def datomic-config (load-config "config/datomic.properties"))
(def db-uri (get datomic-config :datomic.uri))

;; setup && seed database
;(setup-db (load-txs "db/schema.edn"))
(if (d/create-database db-uri)
  (do
    (transact-all (d/connect db-uri) "db/schema.edn")
    (transact-all (d/connect db-uri) "db/seed.edn")))

;; our database connection
(def conn (d/connect db-uri))

(defn get-db [] (d/db conn))

(defn build-txs
  "Converts a namespace (keyword) and a map into a vector of vector assertions.
   This is used internally by entity constructors e.g. `create-participant'.

   e.g. (map->txs :db.part/grade
                  :db/add
                  :participant
                  {:first-name \"Peter\" :last-name \"Parker\"})"
  [part tx ns m]
  (let [id (d/tempid part)
        ks (keys m)
        make-tx (fn [k] [tx id (keyword (name ns) (name k)) (get m k)])]
    (vec (map make-tx ks))))

(defrecord Participant [id first-name last-name])

(defn create-participant
  "Creates participant entity from a map, returns a vector of the form:

       [db uuid]

   where `db' is the updated database and `uuid' is the participant's UUID.

   e.g. (create-participant conn
                            :grade
                            {:first-name \"John\" :last-name \"Smith\"})"
  [conn study proto]
  (let [id (d/squuid)
        part (keyword "db.part" (str "study."(name study)))
        tx @(d/transact conn
                        (build-txs part
                                   :db/add
                                   :participant
                                   (into {:participant-id id} proto)))]
    id))

(defn map->participant [ppt]
  (Participant. (:participant/participant-id ppt)
                (:participant/first-name ppt)
                (:participant/last-name ppt)))

(defn get-participant
  "Find a participant by it's UUID, returns a dynamic map
   of the given participant's attributes or nil if the
   participant cannot be found"
  [db uuid]
  (let [ppt (first (map (fn [eid] (d/entity db eid))
                        (first (d/q '[:find ?p
                                      :where
                                      [?p :participant/participant-id uuid]] db))))]
    (map->participant ppt)))

(defn get-all-participants
  ""
  [db]
  (map (fn [e] (map->participant (d/entity (get-db) (first e))))
       (d/q '[:find ?p :where [?p :participant/participant-id]] (get-db))))

(defn create-study
  "Creates study entity from a map, returns a vector of the form:

       [db name]

   where `db' is the updated database and `name' is the study's
   keyword name.

   e.g. (create-study conn :grade \"GRADE\")"
  [conn name human-name]
  (let [tx (d/transact conn
                       (map->txs :db.part/mea
                                 :db/add
                                 :study
                                 {:name name :human-name name}))]
    [(get tx :db-after) name]))

(defn get-study
  "Find a study by it's keyword name, returns a dyanmic map
   of the given study's attribute or nil of the study cannot be found."
  [db name]
  (first (map (fn [eid] (d/entity db eid))
              (first (d/q '[:find ?s :where [?s :study/name name]] db)))))
