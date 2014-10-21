(ns mea.routes
  (:use compojure.core
        mea.views
        [hiccup.middleware :only (wrap-base-url)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]))

(defroutes main-routes
  (GET "/" [] (index-page))

  ;; participants
  (POST "/participants" {params :params}
        (create-participant params))

  (GET "/participants/:id" [id]
       (get-participant id))

  (GET "/participants" {params :params}
       (prn params)
       (list-participants (params :page) (params :per-page)))

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
