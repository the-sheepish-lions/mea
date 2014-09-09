(defproject mea-service "0.0.1-SNAPSHOT"
  :description "A participant database"
  :url "http://phrei.org"
  :plugins [[lein-marginalia "0.8.0"]
            [lein-ring "0.8.11"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.datomic/datomic-free "0.9.4894"]
                 [org.clojure/data.json "0.2.5"]
                 [hiccup "1.0.5"]
                 [compojure "1.1.9"]
                 [ring/ring-core "1.3.1"]
                 [ring/ring-jetty-adapter "1.3.1"]
                 [ch.qos.logback/logback-classic "1.1.2" :exclusions [org.slf4j/slf4j-api]]]
  :ring {:handler mea.routes/app}
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  :profiles {:dev {:aliases {"run-dev" ["trampoline" "run" "-m" "mea-service.server/run-dev"]}
                   :dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-devel "1.3.1"]]}}
  :global-vars {*print-length* 100}
  :main ^{:skip-aot true} mea.service.server)
