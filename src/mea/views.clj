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
  [ns ks e]
  (->> ks
      (map #(keyword (name ns) (name %1)))
      (map (fn [k] {k (get e k)}))
      (apply conj)))

(defn write-json-uuid [x out]
  (.print out (str "\"" x "\"")))

(extend java.util.UUID json/JSONWriter {:-write write-json-uuid})

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
       (map (fn [p] (e->map :participant [:participant_id :first_name :last_name, :studies] p)))
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
  (->> id
       (java.util.UUID/fromString)
       (core/get-participant (core/get-db))
       (e->map :participant
               [:participant_id
                :vista_key
                :vista_name
                :first_name
                :last_name
                :middle_name
                :studies
                :dob
                :sex
                :address
                :phones
                :emails])
       (json-response)))

(defn create-study
  "Create a study with the given attributes
   return a JSON string representation of of the participant"
  [params]
  (->> {:keyword (keyword (params :keyword)) :human_name (params :human_name)}
       (core/create-study core/conn)
       (e->map :study [:keyword :human_name])
       (json-response)))

(defn list-studies
  "Return full study listing as JSON"
  [page per-page]
  (->> (core/get-all-studies (core/get-db))
       (map #(e->map :study [:keyword :human_name] %1))
       (json-response)))
