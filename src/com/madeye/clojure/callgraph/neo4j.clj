(ns com.madeye.clojure.callgraph.neo4j
  (:gen-class))

(require '[clojurewerkz.neocons.rest :as nr])
(require '[clojurewerkz.neocons.rest.nodes :as nn])
(require '[clojurewerkz.neocons.rest.labels :as nl])
(require '[clojurewerkz.neocons.rest.relationships :as nrl])
(require '[clojurewerkz.neocons.rest.cypher :as cy])
(require '[taoensso.timbre :as timbre])
(require '[clojure.core.async :as async :refer [go <!! chan alts! >!]])

(timbre/refer-timbre)

(defn create-node
  [conn node label]
  (debug "create-node: node=" node ", label=" label)
  (let [node-neo (nn/create conn node)
        name-str (name label)]
    (debug "node-neo:" node-neo ", name-str:" name-str)
    (nl/add conn node-neo (name label))
    node-neo))

(defn label-nodes
  [conn label nodes]
  (dorun (map #(go (nl/add conn % label)) nodes)))

(defn create-nodes
  [conn nodes label]
  (debug "create-nodes: label=" label)
  (let [result (nn/create-batch conn nodes)]
    (label-nodes conn label result)
    result))

;(defn load-method
;  [conn cgclass-neo cgmethod]
;  (debug "load-method:" cgmethod)
;  (let [cgmethod-neo (create-node conn cgmethod :Method)]
;    (debug "cgmethod-neo:" cgmethod-neo)
;    (nrl/create conn cgclass-neo cgmethod-neo :hasMethod)))

;(defn load-class
;  [conn cgclass]
;  (debug "load-class:" cgclass)
;  (let [cgclass-neo (create-node conn { :name (:name cgclass) } :Class)]
;    (dorun (map (partial load-method conn cgclass-neo) (vals (:methods cgclass))))
;    cgclass-neo))

;(defn load-callgraph-slow
;  [conn cg]
;  (info "Loading callgraph (slow) with" (-> cg :classes count) "classes")
;  (dorun (map (partial load-class conn) (vals (:classes cg)))))

(defn create-class-map
  [class-result]
  (reduce merge {} (map #(hash-map (-> % :data :name) (:id %)) class-result)))

(defn- create-has-method-rel
  [conn class-map method-node]
  (let [class-name (-> method-node :data :class-name)
        class-id (get class-map class-name)
        method-id (-> method-node :id)]
    (nrl/create conn class-id method-id :hasMethod)))

(defn load-callgraph
  [conn cg]
  (info "Loading callgraph with" (-> cg :classes count) "classes")
  (let [all-classes (-> cg :classes vals)
        all-methods (reduce into [] (map #(-> % :methods vals) all-classes))
        class-nodes (map #(dissoc % :methods) all-classes)
        class-result (<!! (go (create-nodes conn class-nodes "Class")))
        method-result (<!! (go (create-nodes conn all-methods "Method")))
        class-map (create-class-map class-result)]
     (map #(go (create-has-method-rel conn class-map %)) method-result)))

(defn delete-all-nodes
  ([conn]
    (cy/tquery conn "MATCH (n) DETACH DELETE n")))

(defn count-all-nodes
  ([conn]
    (cy/tquery conn "MATCH (n) RETURN COUNT(n)")))
