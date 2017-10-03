(ns com.madeye.clojure.callgraph.core
  (:gen-class))

(use 'clostache.parser)
(require '[clojurewerkz.neocons.rest :as nr])
(require '[clojurewerkz.neocons.rest.nodes :as nn])
(require '[clojurewerkz.neocons.rest.labels :as nl])
(require '[clojurewerkz.neocons.rest.relationships :as nrl])
(require '[com.madeye.clojure.callgraph.loader :as l])
(require '[com.madeye.clojure.callgraph.filter :as f])
(require '[com.madeye.clojure.callgraph.neo4j :as n])
(require '[clojurewerkz.neocons.rest :as nr])
(require '[taoensso.timbre :as timbre])
(require '[taoensso.timbre.appenders.core :as appenders])
(require '[clojure.core.async :as async :refer [go <!! chan alts! >!]])

(timbre/refer-timbre)

(timbre/merge-config! {:appenders {:spit (appenders/spit-appender {:fname "callgraph.log"})}})
(timbre/merge-config! {:appenders {:println { :enabled? false } }})
(timbre/set-level! :debug)

(defn reload [] (use :reload-all 'com.madeye.clojure.callgraph.core))

(defn -main
  [config-file & args]
  (info "callgraph")
  (let [config (read-string (slurp config-file))
        conn (nr/connect (:neo4j-url config))
        data (l/load-callgraph (:jcg-file config))]
    (n/load-callgraph conn data)))
    
