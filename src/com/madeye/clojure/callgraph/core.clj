(ns com.madeye.clojure.callgraph.core
  (:gen-class))

(use 'clostache.parser)
(require '[com.madeye.clojure.callgraph.loader :as l])
(require '[com.madeye.clojure.callgraph.filter :as f])
(require '[clojurewerkz.neocons.rest :as nr])

(defn reload [] (use :reload-all 'com.madeye.clojure.callgraph.core))

(defn -main
  [config-file & args]
  (let [config (read-string (slurp config-file))
        data (l/parse-callgraph (:jcg-file config))]
    (nr/connect (:neo4j-url config))))
