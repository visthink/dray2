(ns drae.textblock
  "Handle the discovery of textblocks in PDF files."
  (:require [drae.util.math :refer [=* abs-diff]]
            [drae.util.map :refer [apply-fn-map]]
            [drae.util :refer [meta-seq]]
            [drae.doc :refer [vel-x vel-max-x vel-y]]
            )
  )

(declare +textblock-patterns+)

(defn near-same 
  ([myfn margin val1 val2] (and (myfn val1) (myfn val2) (<= (abs-diff myfn val1 val2) margin)))
  ([myfn val1 val2] (near-same myfn 0.01 val1 val2)))

(defn run-pattern [pattern vel-list]
  (println "Pattern is " (:name pattern))
  (println "Vel list is " vel-list)
  (let [mseq (meta-seq #(apply-fn-map (:meta-seq pattern) %1 %2) vel-list)]
    (println "Metasequence is " mseq)
    mseq
  ))















;;; DEFINE TEXTBLOCK PARAGRAPH TYPES

(defn fn-map-test 
  "For each key in fn-map, apply the function value to the corresponding
   entries in the data maps. Return new data map with boolean values."
  [fn-map & data-maps]
  ;;; NOT DONE>
  
  )
  
(def +textblock-patterns+
  {:justified
   {:meta-seq {:same-lhs (partial near-same vel-x)
               :same-rhs (partial near-same vel-max-x)
               :same-font-size (partial near-same #(.font-size (.style %)))
               :spacing #(abs-diff vel-y %1 %2)}
    :seed {:same-rhs true?, :same-lhs true? :same-font-size true?}
    :grow {:same-rhs #(and %1 %2), :same-lhs #(and %1 %2), :same-font-size #(and %1 %2), :spacing =*}
    :front-omit [:same-lhs]
    :back-omit [:same-rhs]
    }
   })
   
