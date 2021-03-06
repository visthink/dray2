(ns dray.domain.numbers
  "Set of recognizers for number sets and sequences."
  (:require [dray.domain.domains :refer :all]
            [dray.domain.recognizers :refer :all]))

(defn integer-value? 
  "True when parameter is an integer, or is a float with an integer value." 
  [x]
  (and (number? x)
       (or (integer? x) (= x (java.lang.Math/floor x)))))

(defDomain Numbers [] 
  "Recognizers for various kinds of number types and patterns."
  
  (defRecognizerTest all-integers "All items are integers" integer?)
  
  )


