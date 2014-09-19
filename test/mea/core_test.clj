(ns mea.core-test
  (:use midje.sweet)
  (:require [mea.core :refer :all]))

(def db-uri "datomic:mem://mea")
(def conn (d/connect db-uri))
(def db (d/db conn))

;;(fact "creates a participant"
;;     (do
;;        (create-participant conn {:first_name "Peter" :last_name "Parker" :vista_name "PARKER,PETER" :dob "1981-11-25"})
;;        (d/q '[:find ?p :where [?p :participant/participant_id]])
