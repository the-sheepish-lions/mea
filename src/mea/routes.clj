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
  (POST "/:study/patients" request
        ;; WARNING: be careful with this, request body is mutable and returns empty after one read! (prn (read-json-request request))
        (let [{params :params} request
              proto (read-transit-str (slurp (get request :body)))]
          (create-ppt (keyword (:study params)) proto)
          ))

  (GET "/:study/patients/:id" [study id]
       (get-ppt (keyword study) id))

  (GET "/:study/patients" {params :params}
       (prn params)
       (list-ppts (keyword (params :study)) (params :page) (params :per-page)))

  ;; studies
  (POST "/" {params :params}
    (prn params)
    (create-study params))

  (GET "/" {params :params} ; {{page :page per-page :per-page} :params}
       (prn params)
       (list-studies (params :page) (params :per-page)))

  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (-> (handler/site main-routes)
      (wrap-base-url)))
