(ns com.madeye.clojure.callgraph.template
  (:gen-class))

(use 'clostache.parser)
(require '[com.madeye.clojure.callgraph.filter :as f])

(defn- render-classgraph
  [data filename]
  (let [class-data (f/filter-class data)
        template-data (hash-map :class-data class-data)
        output (render-resource "templates/class-graph.mustache" template-data)]
    (spit filename output)))

