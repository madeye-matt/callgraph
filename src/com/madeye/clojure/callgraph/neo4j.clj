(ns com.madeye.clojure.callgraph.neo4j
  (:gen-class))

(require '[clojurewerkz.neocons.rest :as nr])
(require '[clojurewerkz.neocons.rest.nodes :as nn])
(require '[clojurewerkz.neocons.rest.relationships :as nrl])
(require '[taoensso.timbre :as timbre])

(timbre/refer-timbre)

(defn load-method
  [conn cgclass-neo cgmethod]
  (debug "load-method:" cgmethod)
  (let [cgmethod-neo (nn/create conn { :name (:name cgmethod) :type :method })]
    (nrl/create conn cgclass-neo cgmethod-neo :hasMethod)))

(defn load-class
  [conn cgclass]
  (debug "load-class:" cgclass)
  (let [cgclass-neo (nn/create conn { :name (:name cgclass) :type :class })]
    (dorun (map (partial load-method conn cgclass-neo) (vals (:methods cgclass))))
    cgclass-neo))

(defn load-callgraph
  [conn cg]
  (info "Loading callgraph with" (-> cg :classes count) "classes")
  (dorun (map (partial load-class conn) (vals (:classes cg)))))
