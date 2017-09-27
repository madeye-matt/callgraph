(ns com.madeye.clojure.callgraph.loader
  (:gen-class))

(use 'clostache.parser)
(require '[clojure.string :as str])

(defn- parse-class
  [class-line]
  (if-let [m (re-matches #"^C:([^ ]*) (.*)$" class-line)]
    (hash-map :type :class :src-class (m 1) :dest-class (m 2))))

(defn get-invocation-type
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
