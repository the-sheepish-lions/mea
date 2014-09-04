(ns mea.core-test
  (:use midje.sweet)
  (:require [mea.core :refer :all]))

(def db-uri "datomic:mem://mea")

(defn tx-future? [f]
  (and
    (contains? f :db-before)
    (contains? f :db-after)
    (contains? f :tx-data)
    (contains? f :tempids)))

(facts "sets up database"
       (tx-future? (setup-db db-uri)) => true)
