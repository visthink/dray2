(ns drae.spreadsheet
  "Read tables encoded in Excel Spreadsheets."
  (:import (com.leidos.bmech.model WorkingSet)
           (org.jfree.chart.renderer AbstractRenderer))
  (:require [clojure.core.matrix :refer :all]
            [incanter.core :refer [to-map to-matrix view $ col-names]]
            [incanter.charts :refer :all]
            [incanter.stats :refer :all]
            [incanter.excel :refer [read-xls]]
            )
  )

(defn spreadsheet-dataset 
  "Read the given spreadsheet file as an Incanter data set."
  [spreadsheet]
  (read-xls spreadsheet :header-keywords true))

(defn spreadsheet-map 
  "Read the given spreadsheet file and return the corresponding map."
  [spreadsheet]
  (-> spreadsheet spreadsheet-dataset to-map))

(defn spreadsheet-matrix
  "Read the given spreadsheet file and return the corresponding matrix."
  [spreadsheet]
  (-> spreadsheet spreadsheet-dataset to-matrix))

(defn describe-columns [dataset]
  
  )



