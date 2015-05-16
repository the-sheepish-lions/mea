(ns mea.service
  (:gen-class)
  (:use compojure.core
        mea.views
        ring.adapter.jetty
        [hiccup.middleware :only (wrap-base-url)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [datomic.api :as d]
            [mea.core :as mea]))

(defn gen-vars
  "Replace data literals with datalog variables"
  [pred]
  (vec (map-indexed (fn [i value]
                      (if-not (or (= clojure.lang.Symbol (class value))
                                  (= clojure.lang.Keyword (class value))
                                  (= clojure.lang.IPersistentList (class value)))
                              (symbol (str "?v" i))
                              value))
                    pred)))

(defn fetch-vars
  [pred]
  (filter #(re-matches #"^\?v\d+" (name %1)) pred))

(defn fetch-vals
  [pred]
  (remove #(or (= clojure.lang.Symbol (class %1))
               (= clojure.lang.Keyword (class %1))
               (= clojure.lang.IPersistentList (class %1))) pred))

;(mapcat fetch-vals '[[?e :ppt/sex "Male"] [[:study/keyword :grade] :study/ppts ?e]])

(defmulti parse-preds 
  "Parse find-entity predicates"
  class)

(defmethod parse-preds
  clojure.lang.IPersistentVector
  [params]
  (let [preds (map gen-vars params)
        vars (mapcat fetch-vars preds)]
    {:values (mapcat fetch-vals params)
     :query
      {:find ['?e]
       :in (vec (concat '($) vars))
       :where preds}}))

(defmethod parse-preds
  clojure.lang.IPersistentMap
  [params]
  (let [vars (map-indexed (fn [i x] (read-string (str "?v" (inc i)))) (keys params))
        preds (vec (map-indexed #(vector '?e %2 (nth vars %1)) (keys params)))]
    {:values (vals params)
     :query
      {:find ['?e]
       :in (vec (concat '($) vars))
       :where preds}}))

(comment

(parse-preds '[[?e :event/type ?t] [[:study/keyword :grade] :study/ppts ?e]])

(filter (fn [pred] (some #(= :#?var %1) pred)) '[[?e :event/type ?t] [:#?var :study/ppts ?e]])

(fetch-vars '[?v1 :event/type ?t])

(re-matches #"^\?v\d+" (name '?v1))

)

(defn find-entities
  "Returns all entities that match predicate"
  [db params]
  (let [pred (parse-preds params)]
    (prn pred)
    (->> (apply (partial d/q (pred :query) db) (pred :values))
         (map first)
         (map (partial d/entity db)))))

(defn update-entity
  "Updates the entity with the given nested attributes"
  [conn id params]
  (-> @(d/transact conn [(into {:db/id id} params)])
      :db-after
      (d/entity id)))

(comment

  (update-entity (mea/get-conn)
                 444202697622585
                 {:ppt/hx
                  {:event/type [:event_type/ident :grade/annual]
                   :event/starts_at #inst "2015-05-04T08:00"
                   :event/ends_at #inst "2015-05-04T12:00"}})

  )

(defroutes main-routes
  ;; generic entity interface everything else is depreciated
  (GET "/entities/:id" [id]
       (-> (d/entity (mea/get-db) (Long/valueOf id))
           e->map
           json-response))
           
  (POST "/find-entities" request
        (let [preds (read-transit-str (slurp (get request :body)))]
          (-> (find-entities (mea/get-db) preds)
              ((fn [es] (map e->map es))) ; FIXME: partial is throwing an exception
              json-response)))

  (POST "/entities/:id" request
        (let [{{id :id} :params body :body} request
              params (read-transit-str (slurp body))]
          (-> (update-entity (mea/get-conn) (Long/valueOf id) params)
              e->map
              json-response)))

  ;; participants
  ;; create
  (POST "/:study/ppts" request
        ;; WARNING: be careful with this, request body is mutable and returns empty after one read! (prn (read-json-request request))
        (let [{params :params} request
              proto (read-transit-str (slurp (get request :body)))]
          (create-ppt (keyword (:study params)) proto)))

  ;; read
  (GET "/:study/ppts/:id" [study id]
       (get-ppt (keyword study) id))

  (GET "/:study/ppts/:id/address" [study id]
       (get-ppt-address (keyword study) id))

  ;; update
  (PUT "/:study/ppts/:id" request
       (let [{params :params} request
             proto (read-transit-str (slurp (get request :body)))]
         (update-ppt (keyword (:study params)) (:id params) proto)))

  ;; delete
  (DELETE "/:study/ppts/:id" [study id]
          (remove-ppt (keyword study) id))

  ;; list
  (GET "/:study/ppts" {params :params}
       (prn params)
       (list-ppts
         (keyword (params :study))
         (clojure.walk/keywordize-keys (dissoc params :study))))

  (POST "/:study/ppts/searches" request
       (let [{params :params} request
             proto (read-transit-str (slurp (get request :body)))]
         (search-ppts (keyword (:study params)) proto)))

  ;; studies
  (POST "/" {params :params}
    (prn params)
    (create-study params))

  (GET "/" {params :params}
       (prn params)
       (list-studies (params :page) (params :per-page)))

  (GET "/:study" {params :params}
       (get-study (keyword (params :study))))

  (route/resources "/")
  (route/not-found "Page not found"))

(def service
  (-> (handler/site main-routes)
      (wrap-base-url)))

(defn -main []
  (do
    (println "Starting Mea on port 3001...")
    (run-jetty service {:port 3001})))
