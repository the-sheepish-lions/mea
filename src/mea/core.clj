(ns mea.core
  (:require [datomic.api :as d]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.spec :as s])
  (:import datomic.Util)
  (:gen-class))

(set! *warn-on-reflection* true)
(def ^:dynamic *mea-env* "development")

;; API
;; 
;; Data Managment
;;  - assert
;;  - retract
;;  - query
;;
;; State Management (levels)
;;
;;  - assert / retract -process
;;  - assert / retract -level
;;  - assert / retract -moment
;;
;; IPC
;;  - send
;;  - receive
;;  - channel
;;
;; Procedural Programming
;;  - Clojure

(defn load-config [file]
  (with-open [^java.io.Reader reader (io/reader file)]
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props] [(keyword k) (read-string v)])))))

(defn read-csv [file]
  (let [data (with-open [in-file (io/reader file)]
               (doall
                 (csv/read-csv in-file)))
        fields (first (take 1 data))]
    (->> (drop 1 data)
         (map (fn [row]
                (->> (map-indexed #(vector (keyword (nth fields %1)) %2) row)
                     (into {})))))))

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
    @(d/transact conn txd))
  :done)

(defn setup-db [db-uri schema]
  (if (d/create-database db-uri)
    (do
      (d/transact (d/connect db-uri) schema)
      :done)
    :already-setup))

(def datomic-config (load-config "config/peer.properties"))
(def db-uri (if (= *mea-env* "production")
                (get datomic-config :datomic.uri)
                "datomic:mem://mea"))

;; our database connection
(defn get-conn [] (d/connect db-uri))

(defn get-db [] (d/db (get-conn)))

(def schema [
  [{:db/id #db/id[:db.part/db -1]
    :db/ident :db.part/mea
    :db.install/_partition :db.part/mea}

   {:db/id #db/id[:db.part/db -2]
    :db/ident :mea/namespace
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent true
    :db/doc "A namespace, required for each mea entity"}
   
   {:db/id #db/id[:db.part/db -22]
    :db/ident :mea.namespace/ident
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/isComponent true
    :db/unique :db.unique/identity}

   {:db/id #db/id[:db.part/db -23]
    :db/ident :mea.namespace/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -24]
    :db/ident :mea.namespace/doc
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -25]
    :db/ident :mea.channel/ident
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/isComponent true
    :db/unique :db.unique/identity}

   {:db/id #db/id[:db.part/db -26]
    :db/ident :mea.channel/name
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -27]
    :db/ident :mea.channel/doc
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -28]
    :db/ident :mea.channel/handler
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent false
    :db/doc "The process that is waiting on messages on the channel"}

   {:db/id #db/id[:db.part/db -3]
    :db/ident :mea/eid
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/isComponent true
    :db/unique :db.unique/identity
    :db/doc "An entity's external ID"}

   {:db/id #db/id[:db.part/db -12]
    :db/ident :mea/type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/isComponent true
    :db/unique :db.unique/value
    :db/doc "An entity's type tag (should correspond to an entity's spec name)"}

   {:db/id #db/id[:db.part/db -4]
    :db/ident :mea.process/ident
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/isComponent true
    :db/unique :db.unique/identity}

   {:db/id #db/id[:db.part/db -5]
    :db/ident :mea.process/string
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -6]
    :db/ident :mea.process/doc
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -10]
    :db/ident :mea.process/levels
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -10]
    :db/ident :mea.process/receptors
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -17]
    :db/ident :mea.receptor/ident
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -18]
    :db/ident :mea.receptor/doc
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -19]
    :db/ident :mea.receptor/lang
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -20]
    :db/ident :mea.receptor/code
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -21]
    :db/ident :mea.receptor/messagePattern
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -7]
    :db/ident :mea.level/ident
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/isComponent true
    :db/unique :db.unique/identity
    :db/doc "A namespaced keyword to identify the level of the form PROCESS_IDENT/LEVEL_IDENT (e.g. :grade.recruitment/0.1)"}

   {:db/id #db/id[:db.part/db -8]
    :db/ident :mea.level/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -9]
    :db/ident :mea.level/doc
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -11]
    :db/ident :mea.level/type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -13]
    :db/ident :mea.level/moments
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -14]
    :db/ident :mea.moment/entity
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent false}

   {:db/id #db/id[:db.part/db -15]
    :db/ident :mea.moment/comment
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/isComponent true}

   {:db/id #db/id[:db.part/db -16]
    :db/ident :mea.moment/refs
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent false}]
])

;; setup database
(setup-db db-uri schema)

;; Data Managment

(defn with-ns
  "Returns a predicate to filter entities from a db object that have the given namespace tag"
  [ns]
  (fn [db datom]
    (= (get (d/entity db (get datom :e)) :mea/namespace) ns)))

;; TODO: create macros defentity and retract-entitydef
;; (defentity process
;;   "Defines a process entity which serves to contain process
;;    state and provide send/receive sematics over channels"
;;   {:mea.process/ident
;;     {:entity/required true
;;      :entity/type :entity.type/keyword}
;;    :mea.process/name
;;     {:entity/required true
;;      :entity/type :entity.type/string}
;;    :mea.process/description
;;     {:entity/type :entity.type/string}})
;;(defmacro defentity
;;  "Defines a Mea entity by doing the following:
;;    1. defining a entity map spec with a name of the form: ':ENTITY' (see clojure.spec)
;;    2. defining a retrieval function named 'ENTITY'
;;    3. defining an assertion function named 'ENTITY-assert' which takes a namespace and a spec map as arguments
;;    4. defining a retraction function named 'ENTITY-retract which takes a namespace and an ident as aguments"
;;  [ident spec])

(defn db? [value]
  (= (class value) datomic.db.Db))

(s/def ::datomic-entity
  (s/keys :req [:db/id]))

(s/def ::entity
  (s/keys :req [:mea/eid :mea/type :mea/namespace]))

(s/fdef entity
  :args (s/cat :ns keyword? :eid (s/or :uuid uuid? :db-id int? :db-ref vector? :db-ident keyword?))
  :ret (s/or :found ::entity :not-found nil?))

(defn entity
  [ns eid]
  (if-let [e (s/assert ::entity (d/entity (get-db) eid))]
    (if-let [ens (get e :mea/namespace)]
        (if (= ns ens) e nil)
        nil)
    nil))

(s/fdef assert
  :args (s/or :fact (s/cat :ns keyword? :eid uuid? :attr keyword? :value any?)
              :spec (s/cat :ns keyword? :eid uuid? :spec map?))
  :ret any?)

(defn assert
  "Assert a fact to the given database"
  ([ns eid spec]
   (d/transact (get-conn)
               (if-let [e (entity ns eid)]
                 [(assoc spec :db/id [:mea/id eid])]
                 [(assoc spec :db/id (d/tempid :db.part/mea) :mea/id eid)])))
  ([ns eid attr value]
    (d/transact (get-conn)
                (if-let [e (entity ns eid)]
                  [[:db/add [:mea/eid eid] attr value]]
                  [(assoc {:db/id (d/tempid :db.part/mea) :mea/id eid}
                          attr
                          value)]))))

(s/fdef retract
  :args (s/or :fact (s/cat :ns keyword? :eid uuid? :attr keyword? :value any?)
              :whole-entity (s/cat :ns keyword? :eid uuid?))
  :ret any?)

(defn retract
  "Retracts a fact from the given database"
  ([ns eid]
   (if-let [e (entity ns eid)]
    (d/transact (get-conn) [[:db.fn/retractEntity (get e :db/id)]])))
  ([ns eid attr value]
   (if-let [e (entity ns eid)]
    (d/transact (get-conn) [[:db/retract (get e :db/id) attr value]]))))

(defn query
  "Perform a query on the given database"
  [ns query]
  (d/q query (filter (get-db) (with-ns ns))))

(s/def ::channel-spec
  (s/keys :req [:mea.channel/ident :mea.channel/name]
          :opt [:mea.channel/description]))

(s/def ::channel
  (s/and ::entity ::channel-spec))

(s/def ::process-spec
  (s/keys :req [:mea.process/ident :mea.process/name]
          :opt [:mea.process/description]))

(s/def ::process
  (s/and ::entity ::process-spec))

(s/def ::level-spec
  (s/keys :req [:mea.level/process :mea.level/ident :mea.level/name]
          :opt [:mea.level/description :mea.level/entityType]))

(s/def ::level
  (s/and ::entity ::level-spec))

(s/def ::moment-spec
  (s/keys :req [:mea.moment/level :mea.moment/entity]
          :opt [:mea.moment/comment :mea.moment/refs]))

(s/def ::moment
  (s/and ::entity ::moment-spec))

(s/fdef channel
  :args (s/cat :ns keyword? :ident keyword?)
  :ret (s/or :found ::channel :not-found nil?))

(defn channel
  "Retrieve channel by ident"
  [ns ident]
  (entity ns [:mea.channel/ident ident]))

(s/fdef channel-assert
  :args (s/cat :ns keyword? :spec ::channel-spec)
  :ret ::channel)

(defn channel-assert
  "Assert a collection of facts (as a spec map) to the db to create a channel"
  [ns spec]
  (assert ns spec))

(s/fdef channel-retract
  :args (s/cat :ns keyword? :ident keyword?)
  :ret any?)

(defn channel-retract
  "Retract a channel from the given database"
  [ns ident]
  (if-let [e (channel ns ident)]
    (d/transact (get-conn) [[:db.fn/retractEntity (get e :db/id)]])))

(s/fdef process
  :args (s/cat :ns keyword? :ident keyword?)
  :ret (s/or :found ::process :not-found nil?))

(defn process
  "Retrieve process by ident"
  [ns ident]
  (entity ns [:mea.process/ident ident]))

(s/fdef process-assert
  :args (s/cat :ns keyword? :spec ::process-spec)
  :ret ::process)

(defn process-assert
  "Assert a collection of facts (as a spec map) to the db to create a process"
  [ns spec]
  (assert ns spec))

(s/fdef process-retract
  :args (s/cat :ns keyword? :ident keyword?)
  :ret any?)

(defn process-retract
  "Retract a process from the given database"
  [ns ident]
  (if-let [e (process ns ident)]
    (d/transact (get-conn) [[:db.fn/retractEntity (get e :db/id)]])))

(s/fdef level
  :args (s/cat :ns keyword? :ident keyword?)
  :ret (s/or :found ::level :not-found nil?))

(defn level
  "Retrieve level by ident"
  [ns ident]
  (entity ns [:mea.level/ident ident]))

(s/fdef level-assert
  :args (s/cat :ns keyword? :spec ::level-spec)
  :ret ::level)

(defn level-assert
  "Assert a collection of facts (as a spec map) to the db to create a level"
  [ns spec]
  (assert ns spec))

(s/fdef level-retract
  :args (s/cat :ns keyword? :ident keyword?)
  :ret any?)

(defn level-retract
  "Retract a level from the given database"
  [ns ident]
  (if-let [e (level ns ident)]
    (d/transact (get-conn) [[:db.fn/retractEntity (get e :db/id)]])))

(s/fdef moment
  :args (s/cat :ns keyword? :ident keyword?)
  :ret (s/or :found ::moment :not-found nil?))

(defn moment
  "Retrieve moment by uuid"
  [ns eid]
  (entity ns eid))

(s/fdef moment-assert
  :args (s/cat :ns keyword? :spec ::level-spec)
  :ret ::moment)

(defn moment-assert
  "Assert a collection of facts (as a spec map) to the db to create a moment"
  [ns spec]
  (assert ns spec))

(s/fdef moment-retract
  :args (s/cat :ns keyword? :ident keyword?)
  :ret any?)

(defn moment-retract
  "Retract a moment from the given database"
  [ns eid]
  (if-let [e (moment ns eid)]
    (d/transact (get-conn) [[:db.fn/retractEntity (get e :db/id)]])))

(defn wait-on
  "Takes a channel and a process that will wait on, and respond to messages from that channel"
  [ch proc])

(comment

  (assert-namespace {:mea.namespace/ident :testing})

)

(comment

  (defprocess scheduling
    "This is a process doc string"
    (receive
      ({:name string?
        :pid string?
        :start-at date?
        :end-at date?
        :eventType? ident?
        :reply channel?}
       (try
        (io->
          (send (channel :channel/vista-scheduling) message)
          (send (channel :add-calendar-event) message)
          (send reply {:status :success :message (str "You've successfully added and event for " (patient pid) " starting on " start-at)}))
       (catch e
        (send reply {:status :error :message (.getMessage e)}))))
      ({:start-at date?
        :end-at date?
        :eventType? ident?
        :reply channel?}
       (try
        (io->
          (send (channel :add-calendar-event) message)
          (send reply (success (str "You've successfully added and event starting on " start-at)))
       (catch e
        (send reply (error (.getMessage e))))))))) 

  (defprocess vista-scheduling
    "This is a doc string"
    (receive
      ({:name string?
        :pid string?
        :start-at date?
        :end-at date?
        :eventType? ident?
        :reply channel?}
       (try
         (io->
           (send
             (channel :shellbot
                      (assoc-in (entity :data/vista-scheduling [:shellbot.script/ident :shellbot.script/schedule-patient])
                                :name name
                                :pid pid
                                :start-at start-at
                                :end-at end-at
                                :eventType eventType)))
          (send reply (success (str "You've successfully added and event for " (patient pid) " starting on " start-at)))))
       (catch e
         (send reply (error (.getMessage e)))))))


  (wait-on (channel :channel/vista-scheduling) vista-scheduling)

)
