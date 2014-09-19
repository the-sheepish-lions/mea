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
  (POST "/participants" {params :params} (create-participant params))
  (GET "/participants/:id" [id] (get-participant id))
  ;(GET "/participants" [page per-page] (list-participants page per-page))
  ;(GET "/participants/study/:study-name" [study-name] (list-participants page per-page))

  ;; studies
  (POST "/studies" {params :params}
    (prn params)
    (create-study params))
  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (-> (handler/site main-routes)
      (wrap-base-url)))
