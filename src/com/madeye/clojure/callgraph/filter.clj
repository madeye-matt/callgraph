(ns com.madeye.clojure.callgraph.filter
  (:gen-class))

(require '[clojure.string :as str])

(defn- filter-data
  [type data]
  (filter #(= (:type %) type) data))
  
(defn filter-class
  [data]
  (filter-data :class data))
  
(defn filter-method
  [data]
  (filter-data :method data))

(defn- reduce-class-map
  [rec1 rec2]
  (let [src-class (:src-class rec2)
        dest-class (:dest-class rec2)
        existing (get rec1 src-class)]
    (if (not (nil? existing))
      (assoc rec1 src-class (conj existing dest-class))
      (assoc rec1 src-class (conj [] dest-class)))))

(defn build-class-map
  [data]
  (let [class-data (filter-class data)]
    (reduce reduce-class-map {} class-data)))

