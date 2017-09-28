(ns com.madeye.clojure.callgraph.loader
  (:gen-class))

(use 'clostache.parser)
(require '[clojure.string :as str])
(require '[clojure.set :as set])

(defrecord Callgraph [classes])
(defrecord CGClass [name methods])
(defrecord CGMethod [name])

(defn- parse-class
  [class-line]
  (if-let [m (re-matches #"^C:([^ ]*) (.*)$" class-line)]
    (hash-map :type :class :src-class (m 1) :dest-class (m 2))))

(defn- get-invocation-type
  [c]
  (case c
    "M" :virtual
    "I" :interface
    "O" :special
    "S" :static))

(defn- parse-method
  [method-line]
  (if-let [m (re-matches #"^M:([^:]*):([^ ]*) \(([A-Z])\)([^:]*):(.*)$" method-line)]
    (hash-map :type :method :src-class (m 1) :src-method (m 2) :invocation-type (get-invocation-type (m 3)) :dest-class (m 4) :dest-method (m 5))))

(defn- parse-line
  [line]
  (let [m (re-matches #"(^[^:]*):.*$" line)]
    ;(println "m(1):" (m 1))
    (case (m 1)
      "M" (parse-method line)
      "C" (parse-class line)
      nil)))

(defn load-data
  [cgfile]
  (let [calldata (str/split-lines (slurp cgfile))]
    (map parse-line calldata)))

(defn create-callgraph
  []
  (Callgraph. {}))

(defn create-class
  [class-name]
  (CGClass. class-name {}))

(defn create-method
  [method-name]
  (CGMethod. method-name))

(defn get-class-from-callgraph
  [cg class-name]
  (get (:classes cg) (keyword class-name)))

(defn add-method-to-class
  [cgclass method-name]
  (if (not (contains? (:methods cgclass) method-name))
    (assoc cgclass :methods (assoc (:methods cgclass) (keyword method-name) (create-method method-name)))
    cgclass))

(defn add-class-to-callgraph
  [cg cgclass]
  (assoc cg :classes (assoc (:classes cg) (keyword (:name cgclass)) cgclass)))

(defn add-class-to-callgraph-if-not-exists
  [cg cgclass]
  (if (nil? (get-class-from-callgraph cg (:name cgclass)))
    (assoc cg :classes (assoc (:classes cg) (keyword (:name cgclass)) cgclass))
    cg))

(defn add-method-to-callgraph
  [cg class-name method-name]
  (let [existing (get-class-from-callgraph cg class-name)]
    (if (not (nil? existing))
      (add-class-to-callgraph cg (add-method-to-class existing method-name))
      (add-class-to-callgraph cg (add-method-to-class (create-class class-name) method-name)))))

(defn- process-callgraph-class-row
  [cg row]
  (-> cg
      (add-class-to-callgraph-if-not-exists (create-class (:src-class row)))
      (add-class-to-callgraph-if-not-exists (create-class (:dest-class row)))))

(defn- process-callgraph-method-row
  [cg row]
  cg)

(defn process-callgraph-row
  [cg row]
  (case (:type row)
    :class (process-callgraph-class-row cg row)
    :method (process-callgraph-method-row cg row)))

(defn process-callgraph
  [data]
  (loop [remaining data cg (create-callgraph)]
    (if (not (empty? remaining))
      (do 
        ;(println "remaining:" (count remaining) ",cg:" cg)
        (recur (rest remaining) (process-callgraph-row cg (first remaining)))
      )
      cg)))

(defn parse-callgraph
  [cgfile]
  (let [data (load-data cgfile)
        class-names (set/union (set (map :dest-class data)) (set (map :src-class data)))]
    (reduce #(add-class-to-callgraph %1 (create-class %2)) (create-callgraph) class-names)))
