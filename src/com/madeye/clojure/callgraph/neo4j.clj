(ns com.madeye.clojure.callgraph.neo4j
  (:gen-class))

(require '[clojurewerkz.neocons.rest :as nr])
(require '[clojurewerkz.neocons.rest.nodes :as nn])
(require '[clojurewerkz.neocons.rest.labels :as nl])
(require '[clojurewerkz.neocons.rest.relationships :as nrl])
(require '[taoensso.timbre :as timbre])

(timbre/refer-timbre)

(defn create-node
  [conn node-name label]
  (debug "create-node: name=" node-name ", label=" label)
  (let [node-neo (nn/create conn { :name node-name })]
    (nl/add conn node-neo (name label))))

(defn load-method
  [conn cgclass-neo cgmethod]
  (debug "load-method:" cgmethod)
  (let [cgmethod-neo (create-node conn (:name cgmethod) :Method)]
    (nrl/create conn cgclass-neo cgmethod-neo :hasMethod)))

(defn load-class
  [conn cgclass]
  (debug "load-class:" cgclass)
  (let [cgclass-neo (create-node conn (:name cgclass) :Class)]
    (dorun (map (partial load-method conn cgclass-neo) (vals (:methods cgclass))))
    cgclass-neo))

(defn load-callgraph
  [conn cg]
  (info "Loading callgraph with" (-> cg :classes count) "classes")
  (dorun (map (partial load-class conn) (vals (:classes cg)))))
