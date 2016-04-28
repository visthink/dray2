(ns dray.util.char
  "Utility functions that operate on or create sequences."
  (:require [clojure.set :refer [union intersection]])
)

(defn char-range 
 "Given a first and last letter, returns a sequence of those letters
   and all the letters in between."
 [first-letter last-letter]
 (map char (range (int first-letter) (+ 1 (int last-letter)))))

(defn char-replacer
  "Given a map from single characters to string expressions, create
   a function that replaces all instances of a single character
   with a string equivalent. 

   Example: ((char-replacer {1 \"one\"}) \"this1\" -> \"thisone\" "
  [char-string-map]
  (fn [s] (apply str (for [c s] (get char-string-map c c)))))

(def ^:private +greek-letters-lower+ (char-range \α \ω))

(def ^:private +greek-letters-upper+ (char-range \Α \Ω))

(def ^:private +greek-letter-names+ 
  '[alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu
    xi omicron pi rho sigma sigma tau upsilon phi chi psi omega])

(defn contains-greek-letter? [s]
  (not (empty? (intersection 
                 (set s) 
                 (union (set +greek-letters-lower+)                                                                  
                        (set +greek-letters-upper+))))))

(def ^:dynamic *greek-map* 
  (apply hash-map 
         (concat (interleave +greek-letters-lower+ +greek-letter-names+)
                 (interleave +greek-letters-upper+ +greek-letter-names+))))

