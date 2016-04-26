(ns drae.textblock
  "Handle the discovery of textblocks in PDF files."
  (:require [drae.util.math :refer [=* abs-diff]]
            [drae.util.map :refer [apply-fn-map]]
            [drae.util :refer [meta-seq pairwise-partition-when pairwise-replace]]
            [drae.doc :refer [vel-x vel-max-x vel-y vel-height make-vtext-block]]
            )
  )

(declare +textblock-patterns+)

(def vtext-margin 5.0) ;; Margin for two margins to be the same.

(defn near-same 
  "True if the two values are within the given margin. If the function
   argument is given, true if the values are within the margin
   when the function is applied to both values."
  ([myfn margin val1 val2] (and (myfn val1) (myfn val2) (<= (abs-diff myfn val1 val2) margin)))
  ([myfn val1 val2] (near-same myfn 0.01 val1 val2)))

(defn run-pattern [pattern vel-list]
  (println "Pattern is " (:name pattern))
  (println "Vel list is " vel-list)
  (let [mseq (meta-seq #(apply-fn-map (:meta-seq pattern) %1 %2) vel-list)]
    (println "Metasequence is " mseq)
    mseq
  ))

(def same-rhs (partial near-same vel-max-x vtext-margin))

(def same-lhs (partial near-same vel-x vtext-margin))

(def same-font-size (partial near-same #(.font-size (.style %)) vtext-margin))

(def same-height (partial near-same vel-height vtext-margin))

(defn same-line-spacing [text1 text2 text3] 
  (=* (- (vel-y text2) (vel-y text1))
      (- (vel-y text3) (vel-y text2))
      vtext-margin))

(defn paragraph-first-line? [vtexts1 vtexts2]
  ;; We assume a potential first line when
  ;;   - Vtexts1 is a single line and vtexts2 is multiple lines.
  ;;   - They are the same font size.
  ;;   - They share a right margin but not a left margin.
  ;;
  (let [line1 (first vtexts1)  ;; Vtext1 line (only one)
        line2 (first vtexts2)] ;; First line of next paragraph.
    (println "Comparing " line1 " and " line2)
    (and (= 1 (count vtexts1))
         (< 1 (count vtexts2))
         (same-rhs line1 line2)
         (not (same-lhs line1 line2))
         (same-font-size line1 line2)
         (let [line3 (nth vtexts2 1)] ;; Second line of next paragraph.
           (same-line-spacing line1 line2 line3)))))

(defn paragraph-last-line? [vtexts1 vtexts2]
  ;; We assume a potential last line when
  ;;   - Vtexts1 is multiple lines and vtexts2 is a single line.
  ;;   - They are the same font size.
  ;;   - They share a left but not a right margin.
  (let [line1 (last vtexts1) ;; Last line in paragraph.
        line2 (first vtexts2)] ;; First line in next paragraph.
    (and (< 1 (count vtexts1))
         (= 1 (count vtexts2))
         (same-font-size line1 line2)
         (same-lhs line1 line2)
         (not (same-rhs line1 line2))
         (let [line0 (nth vtexts1 (- (count vtexts1) 2))] ;; Next to last line in paragraph 1.
           (same-line-spacing line0 line1 line2)))))

(defn simple-vtext-block-recognizer 
  "This is a very simple vtext-block recognizer that simply looks
   at the line spacing. Returns list of vtext-blocks. For now,
   we assume that vtexts are in printing order on the page." 
  [vtexts]
  (let [initial-vtext-partition 
        ;; Partition first by common margins and font size.
        (pairwise-partition-when 
          (fn [text1 text2]
            (not (and (same-rhs text1 text2)
                      (same-lhs text1 text2)
                      (same-font-size text1 text2))))
          vtexts)
        with-merged-first-lines
        (pairwise-replace initial-vtext-partition paragraph-first-line? concat)
        with-merged-last-lines
        (pairwise-replace with-merged-first-lines paragraph-last-line? concat)
        ]
    ;; Return partitioned vtexts as vtext-blocks.
    (mapv make-vtext-block with-merged-last-lines)))


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
   
