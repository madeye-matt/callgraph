(ns callgraph.core
  (:gen-class))

(use 'clostache.parser)
(require '[clojure.string :as str])

(defn reload [] (use :reload-all 'callgraph.core))

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

(defn- load-data
  [cgfile]
  (let [calldata (str/split-lines (slurp cgfile))]
    (map parse-line calldata)))

(defn- filter-data
  [type data]
  (filter #(= (:type %) type) data))
  
(defn- filter-class
  [data]
  (filter-data :class data))
  
(defn- filter-method
  [data]
  (filter-data :method data))

(defn- render-class
  [class-data root-class degrees]
  )

(defn- render-classgraph
  [data root-class degrees filename]
  (let [class-data (filter-class data)
        template-data (hash-map :class-data class-data)
        output (render-resource "templates/class-graph.mustache" template-data)]
    (spit filename output)))

(defn- reduce-class-map
  [rec1 rec2]
  (let [src-class (:src-class rec2)
        dest-class (:dest-class rec2)
        existing (get rec1 src-class)]
    (if (not (nil? existing))
      (assoc rec1 src-class (conj existing dest-class))
      (assoc rec1 src-class (conj [] dest-class))
    )
  )
)

(defn- build-class-map
  [data]
  (let [class-data (filter-class data)]
    (reduce reduce-class-map {} class-data)
  )
)

(defn -main
  [cgfile & args]
  (let [data (load-data cgfile)]
    data))
