;;;;
;;;; Name:      drae.core-test
;;;; Purpose:   Tests for the core code.
;;;; Author:    Ron Ferguson
;;;; Created:   2014-04-22
;;;; Updated:   2014-05-08
;;;;
;;;; 
(ns drae.test-data
  "Test and sample data for use by DRAE."
  (:require [clojure.test :refer :all]
            [clojure.java.io :refer [file]]
            #_[drae.core :refer [corpus #_extracted-pages]]
            )
  (:import (java.util.Arrays.useLegacyMergeSort)))


;;; SAMPLE DATA

(def +sample-xml+
  "<xmi:style name=\"zones\">
  <param name=\"foreground\" value=\"black\"/>
  <param name=\"background\" value=\"yellow\"/>
	<param name=\"transparency\" value=\"1.0\"/>
	<param name=\"fill\" value=\"false\"/>
	<param name=\"resolution\" value=\"10\"/>
	<param name=\"fontfamily\" value=\"arial\"/>
 </xmi:style>")

(def +style-sample3+
  '{:styles
    {top-level       {:params {:resolution 10, :fontfamily arial}}
     highlighter     {:params {:fill true, :transparency 0.4}}
     box-it          {:params {:fill false :transparency 1.0}}
     zones           {:inherits [top-level box-it] 
                      :params {:resolution 11 :foreground black, :background yellow}}
     yellow-style    {:inherits [top-level highlighter]
                      :params {foreground yellow, background yellow}}
     image-style     {:inherits [top-level highlighter]
                      :params {:foreground :darkblue1 :background :darkblue1}}
     textblock-style {:inherits [top-level highlighter]
                      :params {:foreground :darkorange1 :background :darkorange1}}
     block-style     {:inherits [top-level box-it]
                      :params {:foreground :red :background :red :fontweight :bold}}
     }
    :objects
    {segment-group {:class Block2 :params {:style division-style}}
     cluster-text  {:class TextArea :params {:style block-style}}
     text-segment  {:class TextArea :params {:style block-style}}
     cluster       {:class Block3 :params {:style xycut-style}}
     edge          {:class Block3 :params {:style edge-style}}
     text          {:class TextArea :params {:style text-style}}
     text-fragment {:class TextArea :params {:style text-style}}
     }
    :layers
    {"Page Image" {:page-image "output.png"} 
     -Boxed-Item- {:abstract true ;; Abstract layer -- not directly instantiated.
                   :params {:x "{floor(@x)}" :y "{floor(@y)}"
                            :w "{floor(@w)}" :h "{floor(@h)}"
                            :colour "0.0" :font-size "{floor(@font-size)}"}
                   }
     "Clusters"   {:inherits [-Boxed-Item-]
                   :for-each-select text-block
                   :object cluster
                   :params {:text "{text()}"}}
     "Headings"   {:inherits [-Boxed-Item-]
                   :for-each-select "text-block[@type='heading']"
                   :object heading
                   :params {:font-size "{font-size()}"}}
     }
    :handlers
    {popup        {:class PopupFlagger :params {:flag type :allow-clear true}}
     info         {:class Info}
     window       {:class InfoWindow}
     }}
      
  )

(def +style-sample3-output+
  '(
     [xml:style {:name top-level}
      [:param {:name resolution :value 10}]
      [:param {:name fontfamily :value arial}]]
     [xml:style {:name highlighter}
      [:param {:name fill :value true}]
      [:param {:name transparency :value 0.4}]]
     [xml:style {:name box-it}
      [:param {:name fill :value false}]
      [:param {:name transparency :value 1.0}]]
     [xml:style {:name zones}
        [:param {:name resolution :value 11}]
        [:param {:name fontfamily :value aria}]
        [:param {:name fill :value false}]
        [:param {:name transparency :value 1.0}]
        [:param {:name foreground :value black}]
        [:param {:name background :value yellow}]
        ]
     ))

(def +prxml-sample-data+ 
  "Test data for PRXML routines. Based on PDFTOXML output."
  '[:DOCUMENT
    [:METADATA
     [:PDFFILENAME
      "./resources/140626_jun WNT partial vector figure corpus/012_05_pv.pdf"]
     [:PROCESS
      {:name "pdftoxml", :cmd "-noText "}
      [:VERSION {:value "2.0"} [:COMMENT]]
      [:CREATIONDATE "Wed Jul  2 13:35:34 2014\n"]]]
    [:PAGE
     {:width "612", :height "792", :number "1", :id "p1"}
     [:MEDIABOX {:x1 "0", :y1 "0", :x2 "612", :y2 "792"}]
     [:CROPBOX {:x1 "0", :y1 "0", :x2 "612", :y2 "792"}]
     [:BLEEDBOX {:x1 "0", :y1 "0", :x2 "612", :y2 "792"}]
     [:ARTBOX {:x1 "0", :y1 "0", :x2 "612", :y2 "792"}]
     [:TRIMBOX {:x1 "0", :y1 "0", :x2 "612", :y2 "792"}]
     [:IMAGE
      {:id "p1_i1",
       :sid "p1_s77",
       :x "112.636",
       :y "119.444",
       :width "214.423",
       :height "300.829",
       :href "images-in-012_05_pv.xml_data/image-1.jpg",
       :clipZone "p1_c4"}]
     [:IMAGE
      {:id "p1_i2",
       :sid "p1_s82",
       :x "347.773",
       :y "119.213",
       :width "111.014",
       :height "302.442",
       :href "images-in-012_05_pv.xml_data/image-2.jpg",
       :clipZone "p1_c6"}]
     [:IMAGE
      {:id "p1_i3",
       :sid "p1_s87",
       :x "89.1335",
       :y "448.475",
       :width "278.939",
       :height "163.041",
       :href "images-in-012_05_pv.xml_data/image-3.jpg",
       :clipZone "p1_c8"}]
     [:IMAGE
      {:id "p1_i4",
       :sid "p1_s160",
       :x "405.953",
       :y "469.789",
       :width "104.112",
       :height "75.9445",
       :href "images-in-012_05_pv.xml_data/image-4.png",
       :clipZone "p1_c11"}]
     [:include {:href "images-in-012_05_pv.xml_data/image-1.vec"}]]])