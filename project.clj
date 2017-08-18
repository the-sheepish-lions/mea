(defproject mea-service "0.0.1-SNAPSHOT"
  :description "An entity based, workflow, scheduling database"
  :url "http://phrei.org"
  :plugins [[lein-ring "0.8.11"]]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.datomic/datomic-free "0.9.4894"]
                 [compojure "1.1.9"]
                 [ring/ring-core "1.3.1"]
                 [ring/ring-jetty-adapter "1.3.1"]
                 ;[ch.qos.logback/logback-classic "1.1.2" :exclusions [org.slf4j/slf4j-api]]
                 [com.cognitect/transit-clj "0.8.259"]
                 [joda-time/joda-time "2.8.2"]
                 [org.clojure/data.csv "0.1.2"]]
  :ring {:handler mea.service/service}
  :repl {:plugins [[cider/cider-nrepl "0.10.0-SNAPSHOT"]
                   [refactor-nrepl "2.0.0-SNAPSHOT"]]
         :dependencies [[alembic "0.3.2"]
                        [org.clojure/tools.nrepl "0.2.12"]]}
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-devel "1.3.1"]]}
             :uberjar {:aot [mea.service]}}
  :global-vars {*print-length* 100}
  :jvm-opts ["-Xmx1g"])
