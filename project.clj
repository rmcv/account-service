(defproject account-service "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha7"]
                 [ring "1.5.0"]
                 [metosin/compojure-api "1.1.3"]]
  :ring {:handler account-service.core/app}
  :profiles {:dev
             {:plugins [[lein-ring "0.9.7"]]}})
