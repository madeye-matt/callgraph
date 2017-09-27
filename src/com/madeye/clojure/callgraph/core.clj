(ns com.madeye.clojure.callgraph.core
  (:gen-class))

(use 'clostache.parser)
(require '[clojure.string :as str])
(require '[com.madeye.clojure.callgraph.loader :as l])

(defn reload [] (use :reload-all 'callgraph.core))

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
  (let [data (l/load-data cgfile)]
    data))
