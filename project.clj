(defproject mea-service "0.0.1-SNAPSHOT"
  :description "A participant database"
  :url "http://phrei.org"
  :plugins [[lein-marginalia "0.8.0"]
            [lein-ring "0.8.11"]
            [datomic-schema-grapher "0.0.1"]
            [lein-bower "0.5.1"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.datomic/datomic-free "0.9.4894"]
                 [org.clojure/data.json "0.2.5"]
                 [hiccup "1.0.5"]
                 [compojure "1.1.9"]
                 [ring/ring-core "1.3.1"]
                 [ring/ring-jetty-adapter "1.3.1"]
                 [ch.qos.logback/logback-classic "1.1.2" :exclusions [org.slf4j/slf4j-api]]
                 [datomic-schema-grapher "0.0.1"]
                 [prismatic/schema "0.3.1"]
                 [cheshire "5.3.1"]]
  :bower-dependencies [[bootstrap "2.3.1"]
                       [jasmine "2.0.0"]]
  :bower {:directory "resources/js-lib"}
  :ring {:handler mea.routes/app}
  :min-lein-version "2.0.0"
  :main mea.core
  :resource-paths ["config", "resources"]
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-devel "1.3.1"]]}}
  :global-vars {*print-length* 100})
