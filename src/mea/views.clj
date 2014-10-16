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
    (include-css "/js/jasmine/lib/jasmine-2.0.3/jasmine.css")
    (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css")
    (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap-theme.min.css")
    (include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js")
    (include-js "https://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js")
    (include-js "/js/underscore-min.js")
    (include-js "/js/jasmine/lib/jasmine-2.0.3/jasmine.js")
    (include-js "/js/jasmine/lib/jasmine-2.0.3/jasmine-html.js")
    (include-js "/js/jasmine/lib/jasmine-2.0.3/boot.js")
    (include-js "/js/test.js")]
   [:body
    [:div {:class "container"}]]))

(defn ppt->map
  "Converts a participant entity map into a JSON serializable map."
  [e]
  {:id (.toString (core/id-of e)) :name (core/name-of e)})

(defn e->map
  "Converts an entity map into a JSON serializable map."
  [keys e]
  (->> keys
       (map (fn [k] {k (e k)})
       (assoc {}))))

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
  (->> (core/get-all-participants (core/get-db))
       (map (fn [p] (e->map p [:id :first_name :last_name])))
       (json-response)))

(defn create-participant
  "Create a paticiant with the given attributes
   return a JSON string representation of of the participant"
  [params]
  (->> (core/create-participant core/conn :grade params)
       (ppt->map)
       (json-response)))

(defn get-participant
  "Return the JSON representation of a participant"
  [id]
  (json-response (ppt->map (core/get-participant id))))

(defn create-study
  "Create a study with the given attributes
   return a JSON string representation of of the participant"
  [params]
  (->> {:keyword (keyword (params :keyword)) :human_name (params :human_name)}
       (core/create-study core/conn)
       (e->map [:keyword :human_name])
       (json-response)))
