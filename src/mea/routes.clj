(ns mea.routes
  (:use compojure.core
        mea.views
        [hiccup.middleware :only (wrap-base-url)])
  (:require [clojure.data.json :as json]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]))

(defroutes main-routes
  ;; participants
  (POST "/:study/ppts" request
        ;; WARNING: be careful with this, request body is mutable and returns empty after one read! (prn (read-json-request request))
        (let [{params :params} request
              proto (read-transit-str (slurp (get request :body)))]
          (create-ppt (keyword (:study params)) proto)))

  (GET "/:study/ppts/:id" [study id]
       (get-ppt (keyword study) id))

  (GET "/:study/ppts" {params :params}
       (prn params)
       (list-ppts (keyword (params :study)) (params :page) (params :per-page)))

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
