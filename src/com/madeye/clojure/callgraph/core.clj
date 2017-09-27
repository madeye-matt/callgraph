(ns com.madeye.clojure.callgraph.core
  (:gen-class))

(use 'clostache.parser)
(require '[clojure.string :as str])
(require '[com.madeye.clojure.callgraph.loader :as l])
(require '[com.madeye.clojure.callgraph.template :as t])
(require '[com.madeye.clojure.callgraph.filter :as f])
(require '[clojurewerkz.neocons.rest :as nr])
(require '[clojurewerkz.neocons.rest.nodes :as nn])
(require '[clojurewerkz.neocons.rest.relationships :as nrl])

(defn reload [] (use :reload-all 'com.madeye.clojure.callgraph.core))

(defn- connect-neo4j
  [url]
    (nr/connect url)) 

(defn -main
  [config-file & args]
  (let [config (read-string (slurp config-file))
        data (l/load-data (:jcg-file config))]
    (connect-neo4j (:neo4j-url config))))
