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

(defn connect
  [config-file]
  (-> config-file
      slurp
      read-string
      :neo4j-url
      nr/connect))


(defn delete-all-nodes
  ([conn]
    (cy/tquery conn "MATCH (n) DETACH DELETE n")))

(defn count-all-nodes
  ([conn]
    (cy/tquery conn "MATCH (n) RETURN COUNT(n)")))

(defn count-all-relationships
  ([conn]
    (cy/tquery conn "MATCH (n1) -[r]-> (n2) return COUNT(r)")))

(defn delete-all-relationships
  ([conn]
    (cy/tquery conn "MATCH (n1) -[r]-> (n2) DELETE r")))

(defn get-all-nodes
  ([conn label]
   (cy/tquery conn (str "MATCH (n:" label ") return n")))
  ([conn]
   (cy/tquery conn "MATCH (n) return n")))

(defn get-class-by-name
  [conn class-name]
  (cy/tquery conn (str "MATCH (n:Class) where n.name ='" class-name "' return n")))

(defn get-method-by-name
  [conn class-name method-name]
  (cy/tquery conn (str "MATCH (c1:Class)-[rel:hasMethod]->(m1:Method) WHERE c1.name = '" class-name "' AND m1.name = '" method-name "'  RETURN m1")))

(defn create-node
  [conn node label]
  ;(trace "create-node: node=" node ", label=" label)
  (let [node-neo (nn/create conn node)
        name-str (name label)]
    ;(trace "node-neo:" node-neo ", name-str:" name-str)
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

(defn create-class-map
  [class-result]
  (reduce merge {} (map #(hash-map (-> % :data :name) (:id %)) class-result)))

(defn- get-method-map-key
  [class-name method-name]
  (str class-name "::" method-name))

(defn create-method-map
  [method-result]
  ;(dorun (map #(trace %) method-result))
  (reduce merge {} (map #(hash-map (get-method-map-key (-> % :data :class-name) (-> % :data :name)) (:id %)) method-result)))

(defn get-method-id-from-map
  [method-map class-name method-name]
  (get method-map (get-method-map-key class-name method-name)))

(defn- create-relationship
  [conn nodeid-1 nodeid-2 relationship]
  ;(trace "create-relationship:" nodeid-1 relationship nodeid-2)
  (try 
      (nrl/create conn nodeid-1 nodeid-2 relationship)
      true
  (catch clojure.lang.ExceptionInfo e
    (error "Error creating relationship:" e)
    false)))

(defn- force-create-relationship
  [conn nodeid-1 nodeid-2 relationship]
  ;(trace "force-create-relationship:" nodeid-1 relationship nodeid-2)
  (loop [try-again true first-attempt true]
    (if try-again
      (do
        (if-not first-attempt
          (do
            (warn "Retrying" relationship nodeid-1 "->" nodeid-2)
            (Thread/sleep 1000)))
        (recur (not (create-relationship conn nodeid-1 nodeid-2 relationship)) false)))))

(defn- create-has-method-rel
  [conn class-map method-node]
  (let [class-name (-> method-node :data :class-name)
        class-id (get class-map class-name)
        method-id (-> method-node :id)]
    (force-create-relationship conn class-id method-id :hasMethod)))

(defn- create-depends-on-rel
  [conn class-map class-dep]
  (let [src-id (get class-map (:src-class class-dep))
        dest-id (get class-map (:dest-class class-dep))]
    (force-create-relationship conn src-id dest-id :dependsOn)))

(defn- create-calls-rel
  [conn method-map method-dep]
  (let [src-method(get-method-id-from-map method-map (:src-class method-dep) (:src-method method-dep))
        dest-method (get-method-id-from-map method-map (:dest-class method-dep) (:dest-method method-dep))
        invocation-type (:invocation-type method-dep)]
    (force-create-relationship conn src-method dest-method :calls))) 

(defn load-callgraph
  [conn cg]
  (info "Loading callgraph with" (-> cg :classes count) "classes")
  (let [all-classes (-> cg :classes vals)
        all-methods (reduce into [] (map #(-> % :methods vals) all-classes))
        class-nodes (map #(dissoc % :methods) all-classes)
        log (info "Creating class nodes")
        class-result-channel (go (create-nodes conn class-nodes "Class"))
        log (info "Creating method nodes")
        method-result-channel (go (create-nodes conn all-methods "Method"))
        class-result (<!! class-result-channel)
        method-result (<!! method-result-channel)
        log (info "Creating class map")
        class-map (create-class-map class-result)
        method-map (create-method-map method-result)
        class-deps (filter #(= (:type %) :class) (-> cg :dependencies))
        method-deps (filter #(= (:type %) :method) (-> cg :dependencies))
        has-method-channels (map #(go (create-has-method-rel conn class-map %)) method-result)
        depends-on-channels (map #(go (create-depends-on-rel conn class-map %)) class-deps)
        calls-channels (map #(go (create-calls-rel conn method-map %)) method-deps)]
     (mapv <!! has-method-channels)
     (mapv <!! depends-on-channels)
     (mapv <!! calls-channels)
     method-map
     ))
