(ns dray.ssa
  "Simple spatial analysis module. Uses the prxml data structures
   for the diagram labels and blobs to detect some simple spatial
   relationships."
  (:import (java.lang Math))
  (:require
    [dray.xml :refer [prxml-tag prxml-attrs prxml-content prxml-attr]]
    [dray.util.math :refer [abs-diff]]
    )
  )

(defn filter-value-range
  "For the given sequence, return the sequence of all items that 
   contain the given key value within the given range. Does not 
   assume an ordered sequence, so expense is linear with size of list."
  [seq key-fn v-low v-high]
  (filter #(let [v (key-fn %)] (and (>= v v-low) (<= v v-high))) seq)) 

(defn filter-x-range [labels low high]
  (filter-value-range labels #(prxml-attr % :x) low high))

(defn left-sides [labels]
  (sort-by first (into-array (map (fn [label] [(prxml-attr label :x) label]) labels))))

(defn right-sides [labels]
  (sort-by first (into-array (map (fn [label] 
                                    [(+ (prxml-attr label :x)
                                        (prxml-attr label :w)) label]) labels))))

(defn top-sides [labels]
  (sort-by first (into-array (map (fn [label] [(prxml-attr label :y) label]) labels))))


(defn bottom-sides [labels]
  (sort-by first (into-array (map (fn [label] 
                                    [(+ (prxml-attr label :x)
                                        (prxml-attr label :w)) label]) labels))))

(defn h-overlap? [label1 label2]
  ;; Distance from left side to right is less than combined width.
  (let [[x1 w1] (vals (select-keys (prxml-attrs label1) [:x :w]))
        [x2 w2] (vals (select-keys (prxml-attrs label2) [:x :w]))
        combined-width (+ w1 w2)
        left-side-diff (abs-diff x1 x2)
        right-side-diff (abs-diff (+ x1 w1) (+ x2 w2))
        ]
  (> combined-width (+ left-side-diff right-side-diff))
    ; x1 w1 x2 w2 combined-width left-side-diff right-side-diff
    ))

(defn adjoining-labels 
  "Given the PRXML data that contains a label (or set of labels)
   find the set of labels that are touching or close to touching."
  [label-data]
  (let [sort-by-attr (fn [attr] (sort-by #(prxml-attr % attr) label-data))
        sorted-x (into-array (sort-by-attr :x))
        sorted-y (into-array (sort-by-attr :y))
        #_(sort-by #(:x (prxml-attrs %))
                  (filter #(= :Contained-label (prxml-tag %)) label-data))
        ]
    {:sorted-x sorted-x :sorted-y sorted-y}
    ))

