(ns mea.core
  (:require [mea.data :as data]
            [datomic.api :as d]
            [mea.resources :as r]
            [compojure.core :refer [defroutes ANY]])
  (:gen-class))

(defn load-config! [file]
  (with-open [^java.io.Reader reader (clojure.java.io/reader file)]
    (let [props (java.util.Properties.)]
      (.load props reader)
      (into {} (for [[k v] props] [(keyword k) (read-string v)])))))

(def config (load-config! "resources/config.properties"))
(def db-uri (get config :datomic-uri))

;; setup database
(data/setup-db db-uri)

(def conn (d/connect db-uri))
(def db (d/db conn))
