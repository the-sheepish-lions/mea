(defproject mea-service "0.0.1-SNAPSHOT"
  :description "A participant database"
  :url "http://delonnewman.name"
  :plugins [[lein-ring "0.8.11"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.datomic/datomic-free "0.9.5544"]
                 [compojure "1.1.9"]
                 [ring/ring-core "1.3.1"]
                 [ring/ring-jetty-adapter "1.3.1"]
                 [com.cognitect/transit-clj "0.8.259"]
                 [joda-time/joda-time "2.8.2"]
                 [org.clojure/data.csv "0.1.2"]
                 [prismatic/schema "1.1.3"]]
  :ring {:handler mea.service/service}
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-devel "1.3.1"]]}
             :uberjar {:aot [mea.service]}}
  :global-vars {*print-length* 100}
  :jvm-opts ["-Xmx1g"])
