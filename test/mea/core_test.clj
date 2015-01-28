(ns mea.core-test
  (:require [mea.core :refer :all]))

;;(fact "creates a participant"
;;     (do
;;        (create-participant conn {:first_name "Peter" :last_name "Parker" :vista_name "PARKER,PETER" :dob "1981-11-25"})
;;        (d/q '[:find ?p :where [?p :participant/participant_id]])
