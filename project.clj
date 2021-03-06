(defproject com.madeye.clojure.callgraph "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
                 [org.clojure/clojure "1.8.0"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 [tuddman/neocons "3.2.0-SNAPSHOT"]
                 [com.taoensso/timbre "4.10.0"]
                 [org.clojure/core.async "0.3.443"]
                ]
  :main ^:skip-aot com.madeye.clojure.callgraph.core
  :target-path "target/%s"
  :jvm-opts ["-Dclojure.core.async.pool-size=16"]
  :profiles {:uberjar {:aot :all}})
