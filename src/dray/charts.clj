(ns dray.charts
  "*Charts to display useful textual characteristics of a given set of visual elements.
   Can also be used to convert visual elements, working sets, or PDFs into Incanter data sets.*"
  (:import (com.leidos.bmech.model WorkingSet)
           #_(org.jfree.chart.renderer AbstractRenderer)) 
  (:require [incanter.core :refer [$ to-dataset add-derived-column with-data view]]
            [incanter.charts :refer [add-histogram add-points histogram scatter-plot]]
            #_[incanter.stats :refer :all]
            [seesaw.core :refer [frame show!]]
            [dray.util :refer [uerr]]
            [dray.doc :refer [get-vdocument]]
            )
  )

;;; SPECIAL NOTE (4/8/2015): This code contains a lot of repetition, and could be made much more 
;;;                          concise. However, it is a non-critical task.  See especially,
;;;                          text-histogram and its subroutines. 
;;;            
;;;                          pdf->dataset, vels->dataset, and wset->dataset might also 
;;;                          be turned into a single multimethod.  - RWF
;;;

(defn- bbox->map "BBox as map" [b]
  {:x (.x b) :y (.y b) :height (.height b) :width (.width b)})

(defn- vel->map "Visual element as map" [v]
  (let [{:keys [bbox style]} v]
    (merge (dissoc v :items :bbox :style)
           (if bbox (bbox->map bbox) {}) ; Add bbox dims if avail.
           (or style {})) ; Add style if avail.
    ))
  
(defn vels->dataset 
  "Give a list of visual elements, return as an Incanter dataset of their dimensions."
  [vs]
  (to-dataset (map vel->map vs)))
  
(defn wset->dataset
  "Given a working set, return an Incanter dataset of its visual elements."
  [ws]
  (vels->dataset (.getItems ws)))

(defn- augment-bbox-dataset
  "Add additional information - right, bottom, y-inverted, bottom-inverted - to the dataset."
  [ds]
  (->> ds
    (add-derived-column :right [:x :width] +)
    (add-derived-column :bottom [:y :height] +)
    (add-derived-column :y-invert [:y] -)
    (add-derived-column :bottom-invert [:bottom] -)))

(defn pdf->dataset
  "Create an Incanter dataset for the visual elements from a particular pdf."
  [pdf]
  (let [page1-elements (.items (first (.items (get-vdocument pdf))))
        core-data-set (vels->dataset page1-elements)
        ]
    (->> core-data-set
      (add-derived-column :right [:x :width] +)
      (add-derived-column :bottom [:y :height] +))))

(defn text-line-edge-scatter 
  "Create and display a scatter plot of left and right text edges. 
   Data is a dataset of vels as created by any of the `->dataset` functions."
  [data]
  (with-data (augment-bbox-dataset data)
    (let [plot (scatter-plot ($ :x) ($ :y-invert) 
                  :title "Top-left and top-right points for text lines" 
                  :series-label "left edge")]
      (view (add-points plot ($ :right) ($ :y-invert) :series-label "right edge")))))


(defn- text-height-histogram [data]
  (with-data data
    (doto (histogram ($ :height) :nbins 100 :density true :title "Text Height Histogram"
                     :x-label "Text Line Characteristics" :y-label "Density" :legend true
                     :series-label "Text Height")
      (view))))

(defn- text-width-histogram 
  "Create and display a histogram of all the text widths for the visual element data set."
  [data]
  (with-data data
    (doto (histogram ($ :width) :nbins 100 :density true :title "Text Width Histogram"
                     :x-label "Text Line Characteristics" :y-label "Density" :legend true
                     :series-label "Text Width")
      (view))))

(defn- text-lhs-histogram 
  "Create and display a histogram of all the left-hand margins for the visual element data set."
  [data]
  (with-data data
    (doto (histogram ($ :x) :nbins 100 :density true :title "Text LHS Histogram"
                     :x-label "Text Line Characteristics" :y-label "Density" :legend true
                     :series-label "Text LHS")
      (view))))

(defn- text-rhs-histogram 
  "Create and display a histogram of all the right-hand margins for the visual element data set."
  [data]
  (with-data (augment-bbox-dataset data)
    (doto (histogram ($ :right) :nbins 100 :density true :title "Text RHS Histogram"
                     :x-label "Text Line Characteristics" :y-label "Density" :legend true
                     :series-label "Text RHS")
      (view))))

(defn- text-font-size-histogram 
  "Create and display a histogram of all the font sizes in the visual element data set."
  [data]
  (with-data data
    (doto (histogram ($ :font-size) :nbins 100 :density true :title "Font Size Histogram"
                     :x-label "Font size" :y-label "Density" :legend true)
      view)))

(defn text-histogram 
  "Create and display a histogram of the text elements in the given data set. Attributes 
   include: `:font-size`, `:rhs` (right-hand sides of elements), `:lhs` (left-hand-side),
   `:width`, and `:height`."
  ;; This could also be a multimethod, of course - rwf.
  [attribute data]
  (case attribute
    :font-size (text-font-size-histogram data)
    :rhs       (text-rhs-histogram data)
    :lhs       (text-lhs-histogram data)
    :width     (text-width-histogram data)
    :height    (text-height-histogram data)
    (uerr "No text attribute named: %s" attribute)
    ))
