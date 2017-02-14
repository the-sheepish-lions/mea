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

;; API
;; 
;; Data Managment
;;  - assert
;;  - retract
;;  - query
;;
;; State Management (levels)
;;
;;  - assert / retract -scale
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
;;
;; Data Management
;;
;;  POST /data - create data store
;;    params:
;;      - ident (the name of the data store)
;;      - name (the human name of the data store)
;;      - doc (document string to describe data store)
;;
;;  GET /data/:database - return a (lazy) database object
;;    params:
;;      - q (optional, db query)
;;      - asof (optional, timestamp to return a version of the database)
;;  
;;  POST /data/:database/:eid - assert fact to data store
;;    params:
;;      - attribute (required)
;;      - value (required)
;;
;;  DELETE /data/:database/:eid - retract fact from data store (if no attribute or value is specified whole entity is retracted)
;;    params:
;;      - attribute (optional)
;;      - value (optional)
;;
;; State Mangagment
;;
;; POST /process - assert scale
;;  params:
;;    - ident (required)
;;    - name (required)
;;    - description (optional)
;;    - receptors (optional)
;;
;; DELETE /process/:name - retract a scale
;;
;; POST /process/:process/level - assert level
;;  params:
;;    - ident (required)
;;    - name (required)
;;    - description (optional)
;;    - entityType (optional, defaults to :mea.type/any)
;;
;; DELETE /process/:process/level/:level - retract level
;;
;; POST /process/:process/level/:level - assert moment
;;   params:
;;     - entity (ref to mea entity, UUID)
;;     - comment (optional)
;;     - refs (optional, other refs to mea entities)
;;
;; DELETE /scale/:scale/level/:level/:mid - retract moment
;;
;; IPC
;;
;; POST /channel - assert channel
;;  params:
;;    - ident (required)
;;    - name (required)
;;    - description (optional)
;;
;; POST /channel/:channel/receive - assert "receive" procedure to dispatch based on message pattern
;;  params:
;;    - pattern (required)
;;    - procedure (required, map of the following)
;;        - lang
;;        - code
;;
;; POST /channel/:channel/send - send message to channel
;;  params:
;;    - data (required)

(defn pager [page psize col]
  (->> col (drop psize) (take psize)))

(defroutes api
  (POST "/data" {{nm :name ident :ident doc :doc} :body}
        (cond (and nm ident doc) (mea/assert-namespace (keyword ident) nm doc)
              (and nm ident) (mea/assert-namespace (keyword ident) nm)
              (nil? ident) (mea/assert-namespace (keyword ident))
              :else (json-response {:status "success" :data {:name nm :ident ident :doc doc}))
        )

  (GET "/data/:database" {{db :database} :params}
       db)

  (route/resources "/")
  (route/not-found "Page not found"))

(def service
  (-> (handler/site api)
      (wrap-base-url)))

(defn -main []
  (do
    (println "Starting Mea on port 3000...")
    (run-jetty service {:port 3000})))
