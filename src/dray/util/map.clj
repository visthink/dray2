(ns dray.util.map
  "Utility functions for handling maps."
  (:require [dray.util.math :refer [=*]]
            [dray.util :refer [uerr]]
            ))

(defn map-subset? 
  "Are all the key values in map1 also found in map2? Uses
   approximate comparison (=*) for numeric values."
  [map1 map2]
  (let [same-key-val? (fn [[k v]] (=* v (get map2 k)))]
    (every? same-key-val? map1)))

(defn maps-equal? 
  "Do the two maps share the same keys and values?"
  [map1 map2]
  (and (map-subset? map1 map2) (map-subset? map2 map1)))
  
(defn contains-key-vals? 
  "Does the given map contain the given keys and values?
   Uses approximate comparison (=*) for numeric values."
  [map1 & keyvals]
  (map-subset? (apply hash-map keyvals) map1))

(defn mapify "Turn list into map structure." [l]
  (apply hash-map 
     (apply concat
         (map #(list (keyword (first %)) (second %)) (partition 2 l)))))

(defn remove-null-keys "Remove key values that are nil from map." [m]
   (let [non-null-keys (filter #(not (nil? (get m %))) (keys m))]
     (select-keys m non-null-keys)))

(defn select-keys-if 
  "For the given map, return those entries that test true for the test function.
   Test function should take a single key and value from the map."
  [m test-fn]
  (let [selected-keys (for [[k v] m :when (test-fn k v)] k)]
    (select-keys m selected-keys)))

(defn merge-map-keys 
  ([m old-keys new-key merge-fn]
    (if-not (map? m) (uerr "First parameter must be a map: %s" m))
    (let [new-vals (reduce merge-fn (map #(get m %) old-keys))]
      (assoc (apply dissoc m old-keys) new-key new-vals)))
  ([m old-keys new-key] (merge-map-keys m old-keys new-key concat)))

(defn apply-fn-map 
  "Given a map of functions, where the key is a keyword and the value is 
   a function, and a list of args, create a map with the same keys with the 
   values replaced by the application of the key's function to the args.
   
   Example:
   
   `(apply-fn-map {:product *, :sum +} 1 2 3 4)` ; ==> {:product 24, :sum 10}
   "
  [fn-map & args]
  (reduce (fn [m k] (assoc m k (apply (get fn-map k) args))) '{} (keys fn-map)))
