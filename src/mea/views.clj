(ns mea.views
  (:use [hiccup core page]
        [clojure.walk])
  (:require [cognitect.transit :as transit]
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
  [e ks]
  (->> (map (fn [k] {k (get e k)}) ks)
       ((partial conj []))))

(defn write-transit-str [value]
  (let [io (java.io.ByteArrayOutputStream. 4096)
        w (transit/writer io :json-verbose)]
    (transit/write w value)
    (.toString io)))

(defn read-transit-str [s]
  (let [io (java.io.ByteArrayInputStream. 4096)
        r (transit/reader io :json)]
    (prn s)
    (transit/read r)))

(defn read-json-request
  "Parses JSON string from request body and returns a clojure map"
  [request]
  (-> (get request :body)
      (slurp)
      ((fn [m] (prn m) m))
      (read-transit-str)))

(defn json-response
  "A helper function for returning a JSON response"
  [body]
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf8"}
   :body (write-transit-str body)})

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
       (map #(e->map [:ppt/vista/name :ppt/vista/patient_key :ppt/dob :ppt/first_name :ppt/last_name] %1))
       ((fn [m] (prn m) m))
       (json-response)))

(defn create-ppt
  "Create a paticiant with the given attributes
   return a JSON string representation of of the participant"
  [study proto]
  (if (or (empty? proto) (nil? study))
    (json-response {:type "error" :msg (str "study (" (prn-str study) ") should not be nil and proto (" (prn-str proto) ") should not be empty")})
    (->> (core/create-ppt (core/get-conn) study proto)
         (e->map (keys proto))
         (json-response))))

(defn get-ppt
  "Return the JSON representation of a participant"
  [study id]
  (let [ppt (core/get-ppt-from-study (core/get-db) study (java.util.UUID/fromString id))]
    (if (nil? ppt)
      (json-response {:type "error" :msg (str "Couldn't find patient " id " within the " study " study.")})
      (-> (e->map :ppt (keys ppt) ppt)
          ;(nest-namespaced-keys)
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
