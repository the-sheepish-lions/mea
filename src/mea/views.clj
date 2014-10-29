(ns mea.views
  (:use [hiccup core page]
        [clojure.walk])
  (:require [clojure.data.json :as json]
            [mea.core :as core]
            [schema.core :as s]))

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

(defn e->map
  "Converts an entity map into a JSON serializable map."
  [ns ks e]
  (->> (map #(keyword (name ns) (name %1)) ks)
       (map (fn [k] {k (get e k)}))
       (apply conj)))

(defn write-json-uuid [x out]
  (.print out (str "\"" x "\"")))

(defn write-json-ie-date [x out]
  (.print out
          (str (+ 1900 (.getYear x)) "-" (+ 1 (.getMonth x)) "-" (.getDate x)
               "T" (.getHours x) ":" (.getMinutes x) ":" (.getSeconds x) "-" (.getTimezoneOffset x) ":00")))

(extend java.util.UUID json/JSONWriter {:-write write-json-uuid})
(extend java.util.Date json/JSONWriter {:-write write-json-ie-date})

(defn read-json-request
  "Parses JSON string from request body and returns a clojure map"
  [request]
  (->> (get request :body)
       (slurp)
       (json/read-str)
       (keywordize-keys)))

(defn json-response
  "A helper function for returning a JSON response"
  [body]
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf8"}
   :body (json/write-str body)})

(defn remove-namespaced-keys [m]
  (into {} (map (fn [x] [(keyword (name (first x))) (second x)]) m)))

;; TODO: need to create a query parser for this
(defn find-ppts
  "Return Participant Search Results as JSON"
  [query])

(defn list-ppts
  "Return full participant listing as JSON"
  [study page per-page]
  (->> (core/get-all-ppts (core/get-db) study)
       (map #(e->map :ppt [:ppt_id :first_name :last_name] %1))
       ((fn [m] (prn m) m))
       (json-response)))

(defn create-ppt
  "Create a paticiant with the given attributes
   return a JSON string representation of of the participant"
  [study proto]
  (->> (core/create-ppt (core/get-conn) study)
       (e->map :ppt (keys proto))
       (json-response)))

(defn get-ppt
  "Return the JSON representation of a participant"
  [study id]
  (let [ppt (core/get-ppt-from-study (core/get-db) study (java.util.UUID/fromString id))]
    (if (nil? ppt)
      (json-response {:type "error" :msg (str "Couldn't find patient " id " within the " study " study.")})
      (-> (e->map :ppt (keys ppt) ppt)
          (remove-namespaced-keys)
          (json-response)))))

(defn create-study
  "Create a study with the given attributes
   return a JSON string representation of of the participant"
  [params]
  (->> {:keyword (keyword (params :keyword)) :name (params :name)}
       (core/create-study (core/get-conn))
       (e->map :study [:keyword :name])
       (json-response)))

(defn list-studies
  "Return full study listing as JSON"
  [page per-page]
  (->> (core/get-all-studies (core/get-db))
       (map #(e->map :study [:keyword :name] %1))
       (json-response)))
