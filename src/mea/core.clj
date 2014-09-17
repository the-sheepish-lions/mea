(ns mea.core
  (:require [datomic.api :as d]
            [clojure.data.json :as json]
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

(def datomic-config (load-config "config/peer.properties"))
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

(defn entity-dispatch [e]
  (cond (contains? e :participant/participant_id) ::participant
        (contains? e :study/keyword) ::study
        (contains? e :metric/name) ::metric
        (contains? e :measurement/metric) ::measurement))

(defmulti id-of entity-dispatch)
(defmethod id-of nil [e] nil)
(defmethod id-of ::study [e] (get e :study/keyword))
(defmethod id-of ::metric [e] (get e :metric/name))
(defmethod id-of ::participant [e] (get e :participant/participant_id))

(defmulti name-of entity-dispatch)
(defmethod name-of nil [e] "")
(defmethod name-of ::study [e] (get e :study/keyword))
(defmethod name-of ::metric [e] (get e :metric/name))
(defmethod name-of ::measurement [e]
  (str (name-of (get e :measurement/participant))
       " "
       (name-of (get e :measurement/metric))))
(defmethod name-of ::participant [e]
  (str (get e :participant/first_name) " " (get e :participant/last_name)))

(defmulti human-name-of entity-dispatch)
(defmethod human-name-of nil [e] "")
(defmethod human-name-of ::study [e] (get e :study/human_name))
(defmethod human-name-of ::metric [e] (get e :metric/human_name))
(defmethod human-name-of ::measurement [e] (name-of e))
(defmethod human-name-of ::participant [e] (name-of e))

(defn get-participant
  "Find a participant by it's UUID, returns a dynamic map
   of the given participant's attributes or nil if the
   participant cannot be found"
  [db uuid]
  (let [ppt (first (map (fn [eid] (d/entity db eid))
                        (first (d/q '[:find ?p
                                      :in $ ?uuid
                                      :where
                                      [?p :participant/participant_id ?uuid]]
                                    db uuid))))]
    ppt))

(defn add-participant
  "Builds participant entity from a map, adds the entity to, the database
   specified by the given connection, and returns the participant's UUID.

   e.g. (add-participant conn
                         :some-study
                         {:first-name \"John\" :last-name \"Smith\"})"
  [conn study proto]
  (let [uuid (d/squuid)
        part (keyword "db.part.study" (name study))
        tx @(d/transact conn
                        (build-txs part
                                   :db/add
                                   :participant
                                   (into {:participant_id uuid} proto)))]
    [(:db-after tx) uuid]))

(defn create-participant [conn study proto]
  (apply get-participant (add-participant conn study proto)))

(defn get-all-participants
  "Returns all participants"
  [db]
  (map (fn [e] (d/entity db (first e)))
       (d/q '[:find ?p :where [?p :participant/participant_id]] db)))

(defn create-study
  "Creates study entity from a map, returns a vector of the form:

       [db name]

   where `db' is the updated database and `name' is the study's
   keyword name.

   e.g. (create-study conn :grade \"GRADE\")"
  [conn name human-name]
  (let [tx (d/transact conn
                       (build-txs :db.part/mea
                                  :db/add
                                  :study
                                  {:name name :human_name name}))]
    [(get tx :db-after) name]))

(defn get-study
  "Find a study by it's keyword name, returns a dyanmic map
   of the given study's attribute or nil of the study cannot be found."
  [db name]
  (first (map (fn [eid] (d/entity db eid))
              (first (d/q '[:find ?s :where [?s :study/keyword name]] db)))))
