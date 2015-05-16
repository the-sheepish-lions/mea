(ns mea.core
  (:require [datomic.api :as d]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv])
  (:import datomic.Util)
  (:gen-class))

(set! *warn-on-reflection* true)

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

(defn setup-db [db-uri]
  (if (d/create-database db-uri)
    (do
      (transact-all (d/connect db-uri) "db/schema.edn")
      (transact-all (d/connect db-uri) "db/seed.edn")
      :done)
    :already-setup))

(def datomic-config (load-config "config/peer.properties"))
(def db-uri (get datomic-config :datomic.uri))

;; setup && seed database
;;(setup-db db-uri)

;; our database connection
(defn get-conn [] (d/connect db-uri))

(defn get-db [] (d/db (get-conn)))

(defn build-txs
  "Converts a namespace (keyword) and a map into a vector of vector assertions.
   This is used internally by entity constructors e.g. `create-participant'.

   e.g. (build-txs :db.part/grade
                   :db/add
                   {:ppt/first-name \"Peter\" :ppt/last-name \"Parker\"})"
  [{:keys [part tx proto eid]}]
  (let [id (if (nil? eid) (d/tempid part) eid)]
    (vec (map (fn [kv] [tx id (first kv) (second kv)]) proto))))

(defmulti get-ppt
  "Find a participant by it's UUID or Entity ID,
   returns the participants Entity Map"
  (fn [db id] (class id)))
(defmethod get-ppt java.lang.Long [db id] (d/entity db id))
(defmethod get-ppt java.util.UUID [db uuid]
  (->> (d/entid db [:ppt/ppt_id uuid])
       (get-ppt db)))

(defn assert-entity
  "Builds an entity from a map, adds the entity to the database
   specified by the given connection, and returns the transaction map.

   e.g. (assert-entity conn
                       :db.part/mea
                       {:ppt/first-name \"John\" :ppt/last-name \"Smith\"})"
  [conn part proto]
  @(->> (build-txs {:part part
                    :tx :db/add
                    :proto proto})
        (d/transact conn)))

(defn assert-to-entity
  "Builds an entity from a map, adds the entity to the database
   specified by the given connection, and returns the transaction map.

   e.g. (assert-to-entity conn
                          [:study/keyword :some-study]
                          {:study/ppts [:ppt/ppt_id #uuid \"54495d71-780a-4cf5-a97a-f25d05ed2df4\"]})"
  [conn eid proto]
  @(->> (build-txs {:eid eid
                    :tx :db/add
                    :proto proto})
        (d/transact conn)))

(defn assert-ppt
  "Builds participant entity from a map, adds the entity to, the database
   specified by the given connection, and returns a vector of the form.

   e.g. (assert-ppt conn
                    :some-study
                    {:ppt/first-name \"John\" :ppt/last-name \"Smith\"})"
  [conn study proto]
  (let [part (keyword "db.part.study" (name study))
        uuid (d/squuid)
        tx (->> (into {:ppt/ppt_id uuid} proto)
                (assert-entity conn part))]
    [(:db-after tx) uuid]))

(defn bless-into-study
  "Associate a participant with a study"
  [conn study ppt]
  (assert-to-entity conn
                    [:study/keyword study]
                    {:study/ppts (:db/id ppt)}))

(defn create-ppt
  "The application of `assert-ppt`, `get-ppt` and `bless-into-study`
   returns the result of get-ppt."
  [conn study proto]
  (let [ppt (apply get-ppt (assert-ppt conn study proto))]
    (bless-into-study conn study ppt)
    ppt))

(defn get-all-entities
  "Reterns all entities with a given attribute"
  [db attr]
  (->> (d/q '[:find ?e :in $ ?attr :where [?e ?attr]], db attr)
       (map #(d/entity db (first %1)))))

(defn assert-study
  "Creates study entity from a map, returns a vector of the form:

       [db keyword]

   where `db' is the updated database and `keyword' is the study's
   keyword.

   e.g. (create-study conn {:keyword :grade :human_name \"GRADE\"})"
  [conn proto]
  (if (contains? proto :keyword)
    (let [tx (assert-entity conn :db.part/mea :study proto)]
      [(:db-after tx) (:keyword proto)])
    (throw (Exception. "a keyword is required"))))

(defn get-study
  "Find a study by it's keyword name, returns a dynamic map
   of the given study's attribute or nil of the study cannot be found."
  [db kw]
  (->> (d/entid db [:study/keyword kw])
       (d/entity db)))

(defn create-study
  "The application of `assert-study` to `get-study`"
  [conn proto]
  (apply get-study (assert-study conn proto)))

(defn get-all-studies
  "Returns all studies"
  [db]
  (get-all-entities db :study/keyword))

(defn get-study-part
  "Return the datomic partition that corresponds to the study"
  [db study]
  (->> [:study/keyword study]
       (d/entid db)
       (d/part)
       (d/entity db)))

(defn get-all-ppts
  "Returns all participants"
  [db study]
  (let [ppts (:study/ppts (get-study db study))]
    (if (nil? ppts) #{} ppts)))

(defn filter-ppts
  "Returns all participants that match attr/value pairs"
  [db study params]
  (let [vars (map-indexed (fn [i x] (read-string (str "?v" (inc i)))) (keys params))
        preds (vec (concat [['?s :study/keyword '?study]
                            ['?s :study/ppts '?e]]
                           (map-indexed #(vector '?e %2 (nth vars %1)) (keys params))))
        values (vals params)
        q {:find ['?e]
           :with ['?s]
           :in (vec (concat '($ ?study) vars))
           :where preds}]
    (prn values)
    (prn q)
    (->> (apply (partial d/q q db study) values)
         (map first)
         (map (partial d/entity (get-db))))))

(defn remove-ppt-from-study
  "Remove (retract) PPT from database returns the new db and entity map"
  [conn study eid]
  @(d/transact conn [[:db/retract [:study/keyword study] :study/ppt eid]]))

(defn get-ppt-from-study
  "Returns ppt based on UUID if they are in the given study returns nil otherwise"
  [db study uuid]
  (->> (d/q '[:find ?e ?s
              :in $ ?study ?uuid
              :where
              [?e :ppt/ppt_id ?uuid]
              [?s :study/keyword ?study]
              [?s :study/ppts ?e]] db study uuid)
       (ffirst)
       (d/entity db)))
