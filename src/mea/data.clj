(ns mea.data
  (:require [datomic.api :as d])
  (:gen-class))

(def schema-txs [
                 ;; Participants
                 {:db/id #db/id[:db.part/db]
                   :db/ident :participant/participant_id
                   :db/valueType :db.type/uuid
                   :db/cardinality :db.cardinality/one
                   :db/unique :db.unique/value
                   :db/doc "A unique external ID for Patients"
                   :db.install/_attribute :db.part/db}

                  {:db/id #db/id[:db.part/db]
                   :db/ident :participant/vista_name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/unique :db.unique/identity
                   :db/fulltext true
                   :db/doc "The patient's name as seen in VistA (unique, upsertable)"
                   :db.install/_attribute :db.part/db}

                  {:db/id #db/id[:db.part/db]
                   :db/ident :participant/vista_key
                   :db/valueType :db.type/long
                   :db/cardinality :db.cardinality/one
                   :db/unique :db.unique/value
                   :db/doc "The unique value of PATIENT.PATIENT_KEY from the VistA datawarehouse"
                   :db.install/_attribute :db.part/db}

                  {:db/id #db/id[:db.part/db]
                   :db/ident :participant/first_name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/index true
                   :db.install/_attribute :db.part/db}

                  {:db/id #db/id[:db.part/db]
                   :db/ident :participant/last_name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/index true
                   :db.install/_attribute :db.part/db}

                  {:db/id #db/id[:db.part/db]
                   :db/ident :participant/dob
                   :db/valueType :db.type/instant
                   :db/cardinality :db.cardinality/one
                   :db.install/_attribute :db.part/db}

                  {:db/id #db/id[:db.part/db]
                   :db/ident :participant/sex
                   :db/valueType :db.type/instant
                   :db/cardinality :db.cardinality/one
                   :db.install/_attribute :db.part/db}

                  ;; sexes
                  [:db/add #db/id[:db.part/user] :db/ident :participant.sex/male]
                  [:db/add #db/id[:db.part/user] :db/ident :participant.sex/female]

                  {:db/id #db/id[:db.part/db]
                   :db/ident :patient/address
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one
                   :db.install/_attribute :db.part/db}

                  {:db/id #db/id[:db.part/db]
                   :db/ident :patient/phones
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/many
                   :db.install/_attribute :db.part/db}

                  {:db/id #db/id[:db.part/db]
                   :db/ident :patient/emails
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/many
                   :db.install/_attribute :db.part/db}

                  {:db/id #db/id[:db.part/db]
                   :db/ident :patient/studies
                   :db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/many
                   :db/doc "A list of studies the particpant is a part of"
                   :db.install/_attribute :db.part/db}

                  ;; Phones
                  {:db/id #db/id[:db.part/db]
                   :db/ident :phone/name
                   :db/valueType :db.type/keyword
                   :db/cardinality :db.cardinality/one
                   :db.install/_attribute :db.part/db}

                  {:db/id #db/id[:db.part/db]
                   :db/ident :phone/value
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db.install/_attribute :db.part/db}

                  ;; Emails
                  {:db/id #db/id[:db.part/db]
                   :db/ident :email/name
                   :db/valueType :db.type/keyword
                   :db/cardinality :db.cardinality/one
                   :db.install/_attribute :db.part/db}

                  {:db/id #db/id[:db.part/db]
                   :db/ident :email/value
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db.install/_attribute :db.part/db}

                  ;; Studies
                  {:db/id #db/id[:db.part/db]
                   :db/ident :study/keyword
                   :db/valueType :db.type/keyword
                   :db/cardinality :db.cardinality/one
                   :db/unique :db.unique/value
                   :db/doc "The reference name"
                   :db.install/_attribute :db.part/db}

                  {:db/id #db/id[:db.part/db]
                   :db/ident :study/human_name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/doc "A more user friendly name"
                   :db.install/_attribute :db.part/db}

                  {:db/id #db/id[:db.part/db]
                   :db/ident :study/participant_attributes
                   :db/valueType :db.type/keyword
                   :db/cardinality :db.cardinality/many
                   :db/doc "A list of study-specific attributes that can be added to a participant"
                   :db.install/_attribute :db.part/db}])


;; transation functions

;; database setup and manipulation

(defn setup-db [db-uri]
  (do
    (d/create-database db-uri)
    (d/transact (d/connect db-uri) schema-txs)))

(defn delete-db [db-uri]
  (d/delete-database db-uri))

(defn read-db [db-uri]
  (d/db (d/connect db-uri)))

(defn map->txs
  "Converts a namespace (keyword) and a map into a vector of vector assertions.
   This is used internally by entity constructors e.g. `create-participant'.

   e.g. (map->txs :participant {:first_name \"Peter\" :last_name \"Parker\"})"
  [ns m]
  (let [id (d/tempid :db.part/user)
        ks (keys m)
        make-tx (fn [k] [:db/add id (keyword (name ns) (name k)) (get m k)])]
    (vec (map make-tx ks))))

(defn create-participant
  "Creates participant entity from a map, returns a vector of the form:

       [db uuid]

   where `db' is the updated database and `uuid' is the participant's UUID."
  [conn proto]
  (let [id (d/squuid)
        tx (d/transact conn
                       (map->txs :participant
                                 (into {:participant_id id} proto)))]
    [(d/db conn) id]))

(defn get-participant
  "Find a participant by it's UUID, returns a dynamic map
   of the given participant's attributes"
  [db uuid]
  (map (fn [eid] (d/entity db eid))
       (first (d/q '[:find ?p :where [?p :participant/participant_id uuid]]))))
