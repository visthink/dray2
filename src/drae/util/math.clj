(ns drae.util.math
    (:import (java.lang Math))
    (:require [drae.util :refer [uerr]])
    )

(defn abs-diff 
  "For two arguments, absolute difference. For three arguments,
   the absolute difference after applying the first argument (a selector)
   to the two arguments."
  ([a b] 
    {:pre [(number? a) (number? b)]}
    (Math/abs (- a b)))
  ([selector a b]
    ;{:pre [(function? selector)]}
    (Math/abs (- (selector a) (selector b)))))

(defn =* "True if numbers approximate equal within margin (0.001 by default)." 
  ([a b margin] 
   (or (= a b)
       (and (number? a) (number? b)
            (> margin (abs-diff a b)))))
  ([a b] (=* a b 0.001)))

(defn- min-max-helper [numlist min max]
  (let [[n & the-rest] numlist]
    (cond
      (empty? numlist) [min max] ;; Done.
      (> n max) (recur the-rest min n)
      (< n min) (recur the-rest n max)
      :else (recur the-rest min max))))

(defn min-max 
  "Returns array tuple of the min and max numbers in the number list."
  [numlist]
  (when-not (empty? numlist)
    (let [[item1 & the-rest] numlist] (min-max-helper the-rest item1 item1))))



