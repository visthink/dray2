(ns drae.stats
  "Statistical routines for analyzing document structure. Not graphical -- see
   drae.charts for graphical display routines.

   Examples:

   `(groupings :rhs vels)` ;; Groupings by right-hand margin.

   `(groupings :center-x vels)` ;; Group by center x.

   `(frequencies :rhs vels)` ;; Same as groupings, but add frequency counts.

   `(proportions :rhs vels)` ;; Same as frequencies, but adds proportions.

   `grouping-selectors`      ;; Constant containing available grouping selectors.

   For the default grouping selectors, values are grouped together if their value is
   the same when rounded to the nearest tenth (0.1) value. Adjacent groups are then
   merged to handle edge cases. 

   Partial can be used to create special-purpose functions:

   `(def rhs-propotions (partial proportions :rhs))`

  "
  (:import (java.lang Math))
  (:require [drae.util :refer [uerr pairwise-group-by instances]]
            [drae.util.math :refer [abs-diff]]
            [drae.util.map :refer [select-keys-if merge-map-keys]]
            [drae.doc :refer [vel-x vel-y vel-height vel-width vel-max-x vel-center-x]]
            )
  )

;;; HELPER FUNCTIONS

(defn- round-tenths 
  "Round to the tenths place. Returns nil for non-numbers."
  [n] 
  (if (number? n) (double (/ (Math/round (* (double n) 10.0)) 10.0))))

(def texts-in "All VText instances in this collection." (partial instances drae.j.VisualElement.VText))

;;; GROUPING SELECTOR FUNCTIONS
;;;  These are functions that are used to select particular attributues of
;;;  visual elements for grouping.

(def ^{:private true} grouping-selector-table
  "Selector functions that can be referenced by keyword in the groupings, frequencies, and proportions
   functions."
  {:lhs     #(round-tenths (vel-x %))
   :rhs     #(round-tenths (vel-max-x %))
   :height  #(round-tenths (vel-height %))
   :width   #(round-tenths (vel-width %))
   :center-x #(round-tenths (vel-center-x %))
   :font-size #(or (round-tenths (.font-size (.style %))) 0.0)
   })

(defn- grouping-selector "Return selector function for keyword." [k]
  (or (get grouping-selector-table k)
      (uerr "Cannot find grouping selector key: %s." k)))
  
(def grouping-selectors "All grouping selector keywords." (keys grouping-selector-table))

;;; GROUPINGS

(defn- near-miss? [a b] (> 0.11 (abs-diff a b))) 

(defn- near-miss-keys [m]
  {:pre [(map? m)]}
  (pairwise-group-by (->> m keys (remove not) sort) near-miss?))

(defn- merge-close-grouping-keys [groupings]
  {:pre [(map? groupings)] :post [(map? %)]}
  (let [close-key-sets (near-miss-keys groupings)
        ->new-key #(round-tenths (/ (apply + %) (count %)))
        sorted-concat #(sort-by vel-y < (concat %1 %2))
        merge-items #(merge-with sorted-concat %1 %2)]
    (reduce (fn [m close-keys]
              #_(println (format "Old keys: %s" close-keys))
              (merge-map-keys m close-keys (->new-key close-keys) merge-items)
              )
            groupings close-key-sets)))

(defn groupings 
  "Return a map-based table of visual elements by selector. Selector
   can either be a function that takes a visual element and returns a value,
   or a keyword for `grouping-selector` that will return a selector function
   from that table."
  [selector vels]
  {:pre [(coll? vels)] :post [(map? %)]}
  (let [sfn (if (keyword? selector) (grouping-selector selector) selector)]
    (-> (into (sorted-map) (map (fn [[k v]] [k {:items v}]) (group-by sfn vels)))
      merge-close-grouping-keys)))

;;; SORTED FREQUENCY MAPS

(defn- add-frequencies 
  "Add frequency data to a groupings table."
  [groupings-table] 
  (let [add-frequency-to-map #(assoc % :frequency (count (:items %)))]
    (into {} (map (fn [[k m]] [k (add-frequency-to-map m)]) groupings-table))))

(defn- contains-frequencies? [table] (not (nil? (:frequency (first table)))))

(defn vfrequencies 
  "Return an ordered map of value frequencies for the given selector. Removes values for which
   the selector-fn returns nil."
  [selector vels & _] ;; Ignores additional arguments for now.
  (-> (groupings selector vels) add-frequencies))

(defn line-spacing-frequencies 
  "Frequencies for difference in Y for sequential elements. This is broken out separately
   because it involves distances between visual elements (e.g., text rows) rather
   than single-item dimensions. May not be working correctly."
  [vels]
  (let [vertical-distance #(abs-diff vel-y %1 %2)
        text-vels (texts-in vels)]
    (vfrequencies vertical-distance text-vels (rest text-vels))))

  
;;; PROPORTION MAPS

(defn- add-proportions 
  "Given grouping or frequencies map, return corresponding proportion map."
  [table] ; Can also be grouping table with frequency added.
  (let [freq-table (if (contains-frequencies? table) table (add-frequencies table))
        sum (reduce + (map :frequency (vals freq-table)))
        add-proportion-entry (fn [m] (assoc m :proportion (/ (:frequency m) sum)))]
    (into {} (map (fn [[k v]] [k (add-proportion-entry v)]) freq-table))))

(defn proportions 
  "Return an ordered map of value proportions for the given selector."
  [selector vels & _] ;; Ignores additional arguments for now.
  (-> (vfrequencies selector vels) add-proportions))

(defn- contains-proportions? [table] (not (nil? (:proportion (first table)))))

(defn prune-by-proportion 
  "Remove entries that contain less than the cutoff coverage (default: 0.1 = 10% cutoff). Argument is groupings map
   (will also use frequency and proportion data if already in table)."
  ([table cutoff] ; Can also have frequency or grouping data.
   (let [dens-table (if (contains-proportions? table) table (add-proportions table))] 
     (select-keys-if dens-table (fn [k v] (< cutoff (:proportion v))))
     #_(select-keys dens-table (for [[k v] dens-table :when (< 0.1 (:proportion v))] k))
     ))
  ([table] (prune-by-proportion table 0.1)))

(defn print-table "Print out frequency or other map as formatted table."
  [m]
  (doseq [[k v] m]
    (print (format "\nKey: %4.2f  Val: %2.2f" (double k) (double v)))))

(defn text-summary-table 
  "Create a map-based table giving the pruned portions of critical"  
  [vels & {:keys [prune?] :or {:prune false}}]
  (let [prune-if-needed (if prune? prune-by-proportion identity)
        add-feature (fn [m feat] (assoc m feat (prune-if-needed (proportions feat vels))))] 
    (assoc 
      (reduce add-feature {} [:lhs :rhs :width :center-x :height :font-size])
      :name "Text Stats")))
