(defproject mea "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-ring "0.8.11"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.datomic/datomic-free "0.9.4894"]
                 [org.clojure/data.json "0.2.5"]
                 [compojure "1.1.8"]
                 [ring/ring-core "1.3.1"]
                 [liberator "0.12.1"]
                 [midje "1.6.3"]]
  :main ^:skip-aot mea.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
