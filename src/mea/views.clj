(ns mea.views
  (:use [clojure.walk])
  (:require [cognitect.transit :as transit]
            [datomic.api :as d]
            [mea.core :as core]))

(defn e->map
  "Converts an entity map into a JSON serializable map."
  [e & fields]
  ;; FIXME: This should be rewritten
  (let [ks (if (nil? (first fields)) (keys e) (first fields))]
    (into {:db/id (:db/id e)} (map (fn [k]
          (let [v (get e k)]
            (cond
              (instance? datomic.query.EntityMap v) [k (e->map v)]
              (instance? clojure.lang.PersistentHashSet v) [k (map e->map v)]
              :else [k v]))) ks))))

(defn nsed-name 
  "Convert a keyword or symbol to a string in
   namespace/name format."
  [^clojure.lang.Named kw-or-sym]
  (if-let [ns (.getNamespace kw-or-sym)]
    (str ns "/" (.getName kw-or-sym))
    (.getName kw-or-sym)))

(defn entity? [v] (instance? datomic.query.EntityMap v))

(defn entity-rep [v]
  (into (sorted-map)
    (map
      (fn [[k v]]
        [k ;;(nsed-name k)
         (cond
           (entity? v) (entity-rep v)
           (set? v) (map entity-rep v)
           :else v)]) v)))

(def entity-writer
  (transit/write-handler
    (constantly "map")
    entity-rep
    (fn [v] nil)))

(defn write-transit-str [value]
  (let [io (java.io.ByteArrayOutputStream. 4096)
        w (transit/writer io :json-verbose {:handlers {datomic.query.EntityMap entity-writer}})]
    (transit/write w value)
    (.toString io)))

(defn read-transit-str [s]
  (let [io (java.io.StringBufferInputStream. s)
        r (transit/reader io :json)]
    (transit/read r)))

(defn json-response
  "A helper function for returning a JSON response"
  [body]
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf8"}
   :body (write-transit-str body)})

(defn remove-namespaced-keys [m]
  (into {} (map (fn [x] [(keyword (name (first x))) (second x)]) m)))

;; TODO: need to create a query parser for this
(defn find-ppts
  "Return Participant Search Results as JSON"
  [query])

(defn filter-ppts
  "Filter ppts by attr value pair"
  [study params])

(defn list-ppts
  "Return full participant listing as JSON"
  [study params]
  (->> (core/get-all-ppts (core/get-db) study)
       (map #(e->map %1 [:ppt/vista/name :ppt/vista/patient_key :ppt/dob :ppt/first_name :ppt/last_name :ppt/ppt_id]))
       (json-response)))

(defn search-ppts
  [study params]
  (->> (core/filter-ppts (core/get-db) study params)
       (map #(e->map %1 [:ppt/vista/name :ppt/vista/patient_key :ppt/dob :ppt/first_name :ppt/last_name :ppt/ppt_id]))
       (json-response)))

(defn create-ppt
  "Create a paticiant with the given attributes
   return a JSON string representation of of the participant"
  [study proto]
  (if (or (empty? proto) (nil? study))
    (json-response {:type "error" :msg (str "study (" (prn-str study) ") should not be nil and proto (" (prn-str proto) ") should not be empty")})
    (-> (core/create-ppt (core/get-conn) study proto)
        (e->map (keys proto))
        (json-response))))

(defn get-ppt
  "Return the JSON representation of a participant"
  [study id]
  (let [ppt (core/get-ppt-from-study (core/get-db) study (java.util.UUID/fromString id))]
    (if (nil? ppt)
      (json-response {:type "error" :msg (str "Couldn't find patient " id " within the " study " study.")})
      (json-response (e->map ppt (keys ppt))))))

(defn update-ppt
  "Update PPT and return JSON"
  [study id proto]
  (let [ppt (core/get-ppt-from-study (core/get-db) study (java.util.UUID/fromString id))]
    (if (nil? ppt)
      (json-response {:type "error" :msg (str "Couldn't find patient " id " within the " study " study.")})
      (do
        (core/assert-to-entity (:db/id ppt) proto)
        (core/get-ppt (core/get-db) id)
        (json-response (e->map ppt (keys ppt)))))))

(defn remove-ppt
  "Retract PPT and return JSON"
  [study id]
  (let [ppt (core/get-ppt-from-study (core/get-db) study (java.util.UUID/fromString id))]
    (if (nil? ppt)
      (json-response {:type "error" :msg (str "Couldn't find patient " id " within the " study " study.")})
      (try
        (core/remove-ppt-from-study (core/get-conn) study (:db/id ppt))
        (json-response (e->map ppt (keys ppt)))
        (catch Exception e (json-response {:type "error" :msg (str "Error: " (.getMessage e))}))))))


(defn get-ppt-address
  "Return the JSON representation of a participant's address"
  [study id]
  (let [ppt (core/get-ppt-from-study (core/get-db) study (java.util.UUID/fromString id))]
    (if (nil? ppt)
      (json-response {:type "error" :msg (str "Couldn't find patient " id " within the " study " study.")})
      (let [addr (:ppt/address ppt)]
        (if (nil? addr)
          (json-response nil)
          (json-response (e->map addr (keys addr))))))))

(defn create-study
  "Create a study with the given attributes
   return a JSON string representation of of the participant"
  [params]
  (->> {:keyword (keyword (params :keyword)) :name (params :name)}
       (core/create-study (core/get-conn))
       (e->map [:study/keyword :study/name])
       (json-response)))

(defn list-studies
  "Return full study listing as JSON"
  [page per-page]
  (->> (core/get-all-studies (core/get-db))
       (map #(e->map %1 [:study/keyword :study/name]))
       (json-response)))

(defn get-study
  "Return a more detailed view of a study"
  [study]
  (let [s (core/get-study (core/get-db) study)]
    (-> {:study/name (get s :study/name)
         :study/keyword (get s :study/keyword)
         :study/attributes (map (fn [e]
                                  {:keyword (get e :study.attribute/keyword)
                                   :name (get e :study.attribute/name)
                                   :order (get e :study.attribute/order)}) (get s :study/attributes))
         :study/ppt_count (count (get s :study/ppts))}
        (json-response))))
