(ns mea.routes
  (:use compojure.core
        mea.views
        [hiccup.middleware :only (wrap-base-url)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]))

(defroutes main-routes
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

(def app
  (-> (handler/site main-routes)
      (wrap-base-url)))
