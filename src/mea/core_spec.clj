(ns mea.core-spec
  (:use 'mea.core)
  (:require [datomic.api :as d]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.spec :as s])
  (:import datomic.Util)
  (:gen-class))

;; API
;; 
;; Data Managment
;;  - assert
;;  - retract
;;  - query
;;
;; State Management (levels)
;;
;;  - assert / retract -process
;;  - assert / retract -level
;;  - assert / retract -moment
;;
;; IPC
;;  - send
;;  - receive
;;  - channel
;;
;; Procedural Programming
;;  - Clojure

(s/def ::datomic-entity
  (s/keys :req [:db/id]))

(s/def ::entity
  (s/keys :req [:mea/eid :mea/type :mea/namespace]))

(s/fdef entity
  :args (s/cat :ns keyword? :eid (s/or :uuid uuid? :db-id int? :db-ref vector? :db-ident keyword?))
  :ret (s/or :found ::entity :not-found nil?))

(s/fdef assert
  :args (s/or :fact (s/cat :ns keyword? :eid uuid? :attr keyword? :value any?)
              :spec (s/cat :ns keyword? :eid uuid? :spec map?))
  :ret any?)

(s/fdef retract
  :args (s/or :fact (s/cat :ns keyword? :eid uuid? :attr keyword? :value any?)
              :whole-entity (s/cat :ns keyword? :eid uuid?))
  :ret any?)

(s/def ::channel-spec
  (s/keys :req [:mea.channel/ident :mea.channel/name]
          :opt [:mea.channel/description]))

(s/def ::channel
  (s/and ::entity ::channel-spec))

(s/def ::process-spec
  (s/keys :req [:mea.process/ident :mea.process/name]
          :opt [:mea.process/description]))

(s/def ::process
  (s/and ::entity ::process-spec))

(s/def ::level-spec
  (s/keys :req [:mea.level/process :mea.level/ident :mea.level/name]
          :opt [:mea.level/description :mea.level/entityType]))

(s/def ::level
  (s/and ::entity ::level-spec))

(s/def ::moment-spec
  (s/keys :req [:mea.moment/level :mea.moment/entity]
          :opt [:mea.moment/comment :mea.moment/refs]))

(s/def ::moment
  (s/and ::entity ::moment-spec))

(s/fdef channel
  :args (s/cat :ns keyword? :ident keyword?)
  :ret (s/or :found ::channel :not-found nil?))

(s/fdef channel-assert
  :args (s/cat :ns keyword? :spec ::channel-spec)
  :ret ::channel)

(s/fdef channel-retract
  :args (s/cat :ns keyword? :ident keyword?)
  :ret any?)

(s/fdef process
  :args (s/cat :ns keyword? :ident keyword?)
  :ret (s/or :found ::process :not-found nil?))

(s/fdef process-assert
  :args (s/cat :ns keyword? :spec ::process-spec)
  :ret ::process)

(s/fdef process-retract
  :args (s/cat :ns keyword? :ident keyword?)
  :ret any?)

(s/fdef level
  :args (s/cat :ns keyword? :ident keyword?)
  :ret (s/or :found ::level :not-found nil?))

(s/fdef level-assert
  :args (s/cat :ns keyword? :spec ::level-spec)
  :ret ::level)

(s/fdef level-retract
  :args (s/cat :ns keyword? :ident keyword?)
  :ret any?)

(s/fdef moment
  :args (s/cat :ns keyword? :ident keyword?)
  :ret (s/or :found ::moment :not-found nil?))

(s/fdef moment-assert
  :args (s/cat :ns keyword? :spec ::level-spec)
  :ret ::moment)

(s/fdef moment-retract
  :args (s/cat :ns keyword? :ident keyword?)
  :ret any?)
