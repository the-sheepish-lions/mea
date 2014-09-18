(ns mea.views
  (:use [hiccup core page])
  (:require [clojure.data.json :as json]
            [mea.core :as core]))

(defn index-page
  "Display the Mea Web Service Documentation as HTML"
  []
  (html5 {:lang "en"}
   [:head
    [:title "Mea - A Participant Database"]
    (include-css "/css/style.css")
    (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css")
    (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap-theme.min.css")
    (include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js")
    (include-js "https://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js")
    ]
   [:body
    [:div {:class "container"}
     [:div {:class "page-header"}
      [:h1 "Welcome!"]
      [:small "This is the Mea Web Service Documentation"]]]]))

(defn ppt->map
  "Converts a participant entity map into a JSON serializable map."
  [e]
  {:id (.toString (core/id-of e)) :name (core/name-of e)})

(defn json-response
  "A helper function for returning a JSON response"
  [body]
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf8"}
   :body (json/write-str body)})

;; TODO: need to create a query parser for this
(defn find-participants
  "Return Participant Search Results as JSON"
  [query])

(defn list-participants
  "Return full participant listing as JSON"
  [page per-page]
  (json-response (map ppt->map
                      (core/get-all-participants (core/get-db)))))

(defn create-participant
  "Create a paticiant with the given attributes
   return a JSON string representation of of the participant"
  [body]
  (let [args (json/read-str body :key-fn (fn [k] (keyword k)))]
    (json-response (map ppt->map
                        (core/create-participant core/conn :grade args)))))

(defn get-participant
  "Return the JSON representation of a participant"
  [id])
