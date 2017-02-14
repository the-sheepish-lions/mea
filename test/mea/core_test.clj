(ns mea.core-test
  (:require [clojure.test :refer :all]
            [mea.core :refer :all]
            [datomic.api :as d]))

(setup-db db-uri schema)

(d/transact (get-conn)
            [{:db/id #db/id[:db.part/db -1]
              :db/ident :drn.client/name
              :db/valueType :db.type/string
              :db/unique :db.unique/value
              :db/cardinality :db.cardinality/one}])

(assert-namespace :mea/test "Testing Mea")

(defentity client)
(def cid0 (assert-client :mea/test {:drn.client/name "Briton Leap"}))
(def cid1 (assert-client :mea/test {:drn.client/name "PHREI"}))
(def cid2 (assert-client :mea/test {:drn.client/name "UNM Community Health Center"}))

(assert-level :mea/test {:mea.level/ident :drn.billing/c0.0
                         :mea.level/name "Initialize client into billing system (specify billing category for client)"})

(assert-level :mea/test {:mea.level/ident :drn.billing/c0.1
                         :mea.level/name "Client has been initialized into billing system"})

(assert-level :mea/test {:mea.level/ident :drn.billing/c1.0
                         :mea.level/name "Create new invoice for client"})

(assert-level :mea/test {:mea.level/ident :drn.billing/c1.1
                         :mea.level/name "Invoice created"})

(assert-level :mea/test {:mea.level/ident :drn.billing/c2.0
                         :mea.level/name "Send invoice to client"})

(assert-level :mea/test {:mea.level/ident :drn.billing/c2.1
                         :mea.level/name "Invoice sent to client"})

(assert-level :mea/test {:mea.level/ident :drn.billing/c3.0
                         :mea.level/name "Payment has been recieved"})

(assert-receptor :mea/test {:mea.receptor/doc "Print incoming entities to stdout"
                            :mea.receptor/pattern "{:mea/eid s/Int}"
                            :mea.receptor/lang :lang/clojure
                            :mea.receptor/code "(fn [e] (prn e))"})

(assert-process :mea/test {:mea.process/ident :drn/billing
                           :mea.process/name "Billing process for D.R. Newman"
                           :mea.process/levels
                            [[:mea.level/ident :drn.billing/c0.0]
                             [:mea.level/ident :drn.billing/c0.1]
                             [:mea.level/ident :drn.billing/c1.0]
                             [:mea.level/ident :drn.billing/c1.1]
                             [:mea.level/ident :drn.billing/c2.0]
                             [:mea.level/ident :drn.billing/c2.1]
                             [:mea.level/ident :drn.billing/c3.0]]})

(def d (java.util.Date.))
(def l (level :mea/test [:mea.level/ident :drn.billing/c0.0]))
(def c (client :mea/test cid0))

(def mid
  (assert-moment :mea/test {:mea.moment/level (:db/id l)
                            :mea.moment/entity (:db/id c)
                            :mea.moment/time d}))

(def m (moment :mea/test mid))

(testing "Assertions"
  (is (= (:mea/eid m) mid))
  (is (= (:mea.namespace/ident (:mea/namespace m)) :mea/test))
  (is (= d (:mea.moment/time m)))
  (is (= l (:mea.moment/level m)))
  (is (= c (:mea.moment/entity m)))
  )
