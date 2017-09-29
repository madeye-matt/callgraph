(ns com.madeye.clojure.callgraph.neo4j
  (:gen-class))

(require '[clojurewerkz.neocons.rest :as nr])
(require '[clojurewerkz.neocons.rest.nodes :as nn])
(require '[clojurewerkz.neocons.rest.labels :as nl])
(require '[clojurewerkz.neocons.rest.relationships :as nrl])
(require '[clojurewerkz.neocons.rest.cypher :as cy])
(require '[taoensso.timbre :as timbre])

(timbre/refer-timbre)

(defn create-node
  [conn node-name label]
  (debug "create-node: name=" node-name ", label=" label)
  (let [node-neo (nn/create conn { :name node-name })
        name-str (name label)]
    (debug "node-neo:" node-neo ", name-str:" name-str)
    (nl/add conn node-neo (name label))
    node-neo))

(defn load-method
  [conn cgclass-neo cgmethod]
  (debug "load-method:" cgmethod)
  (let [cgmethod-neo (create-node conn (:name cgmethod) :Method)]
    (debug "cgmethod-neo:" cgmethod-neo)
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

(defn neo4j-delete-all-nodes
  ([conn]
    (cy/tquery conn "MATCH (n) DETACH DELETE n")))

(defn neo4j-count-all-nodes
  ([conn]
    (cy/tquery conn "MATCH (n) RETURN COUNT(n)")))
