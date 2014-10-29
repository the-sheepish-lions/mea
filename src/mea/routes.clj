(ns mea.routes
  (:use compojure.core
        mea.views
        [hiccup.middleware :only (wrap-base-url)])
  (:require [clojure.data.json :as json]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]))

(defroutes main-routes
  (GET "/" [] (index-page))

  ;; participants
  (POST "/:study/patients" request
        ;;(prn (read-json-request request))
        (let [{study :study proto :participant} (read-json-request request)]
          (prn study)
          (prn proto)
          (create-ppt (keyword study) proto)))

  (GET "/:study/patients/:id" [study id]
       (get-ppt (keyword study) id))

  (GET "/:study/patients" {params :params}
       (prn params)
       (list-ppts (keyword (params :study)) (params :page) (params :per-page)))

  ;; studies
  (POST "/studies" {params :params}
    (prn params)
    (create-study params))

  (GET "/studies" {params :params} ; {{page :page per-page :per-page} :params}
       (prn params)
       (list-studies (params :page) (params :per-page)))

  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (-> (handler/site main-routes)
      (wrap-base-url)))
