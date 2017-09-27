(ns com.madeye.clojure.callgraph.loader
  (:gen-class))

(use 'clostache.parser)
(require '[clojure.string :as str])

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

(defn- load-data
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

(defn add-method-to-class
  [cgclass method-name]
  (if (not (contains? (:methods cgclass) method-name))
    (assoc cgclass :methods (assoc (:methods cgclass) (keyword method-name) (create-method method-name)))
    cgclass))

(defn add-method-to-callgraph
  [cg class-name method-name])

(defn add-class-to-callgraph
  [cg class-name]
  (if (not (contains? (:classes cg) class-name))
    (assoc cg :classes (assoc (:classes cg) (keyword class-name) (create-class class-name)))))

(defn parse-callgraph
  [cgfile]
  (let [data (load-data cgfile)
        class-names (clojure.set/union (set (map :dest-class data)) (set (map :src-class data)))]
    (reduce #(add-class-to-callgraph %1 %2) (create-callgraph) class-names)))
