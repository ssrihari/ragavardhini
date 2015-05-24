(defproject movertone "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [overtone "0.9.1"]
                 [leipzig "0.8.1"]
                 [org.clojure/java.jdbc "0.3.7"]
                 [postgresql/postgresql "9.3-1101.jdbc4"]
                 [medley "0.5.5"]
                 [hiccup "1.0.5"]
                 [bidi "1.19.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [metosin/ring-http-response "0.4.0"]
                 [ring-middleware-format "0.3.2" :exclusions [ring]]]
  :plugins [[cider/cider-nrepl "0.8.2"]])
