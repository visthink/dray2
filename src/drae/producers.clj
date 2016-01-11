(ns drae.producers
  "Routines for handling Producers in the DRAE system.

   A Producer is something that can create a representation layer from an element or 
   set of elements. Producers can be loaded and modified at run-time, and are indexed
   by their name."
  (:import (java.io File)
           (com.leidos.bmech.gui ViewerApp)
           (com.leidos.bmech.model WorkingSet Layer LayerList)
          #_ (com.leidos.bmech.analysis TextAnalyzer)
           )
  (:require [drae.j.Producer :refer [add-ws-producer add-layer-producer]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :refer [file]]
            [drae.manager :refer [make-layer]]
            [drae.wset :refer [ws-images ws-make-child]]
            [drae.ext.bee :refer [bee-rep-for]]
            [drae.stats :refer [text-summary-table texts-in]]
            [drae.vtable :refer [ws-vtables extract-vtables simplify-table]]
            [drae.tablerep :refer [rep-table]]
            )
  (:gen-class)
  )

(defn pv-diagram-selection-producer 
  "Given a WorkingSet, produce new working sets with the images 
   and overlapping visual elements."
  [ws]
  (into '() ;; return as list
        (for [vimage (ws-images ws)]
          (ws-make-child ws "Producer: Diagram Image Selector" (:bbox vimage)))))

(defn bee-layer-producer-ws "Run BEE on the current working set to produce a layer." [ws]
  (let [vimage (first (ws-images ws))
        pdf-file (file (.getFilename ws))
        image-filename (.bitmap-path vimage)]
    (let [layer (make-layer (str "BEE analysis on " image-filename)
                            (list (bee-rep-for pdf-file image-filename)))]
      (println (str "\n Returning LAYER " layer))
      (list layer))))

#_(defn gene-layer-producer-ws "Run Arizona Processor on the current working set to produce a layer." [ws]
  (list (TextAnalyzer/produceGeneLayer ws)))

(defn text-stats-producer-ws "Run stats on texts in current working set to produce a layer." [ws]
  (let [texts (texts-in (.getItems ws))
        summary-table (text-summary-table texts)]
    (list (make-layer (str "Text Stats for " (.getName ws)) (list summary-table)))))


(defn manual-table-producer-ws 
  "Attempt to construct a table from manually-labelled table ws." 
  [ws]
  (let [tabs (extract-vtables ws)]
    (list (make-layer (str "Labeled tables in " (.getName ws)) tabs))))

(defn simple-table-producer-ws 
  "Create a simplified table representation with the visual elements
   and bounding boxes stripped out."
  [ws]
  (let [tabs (extract-vtables ws)]
    (list (make-layer (str "Simplified Tables in " (.getName ws))
                      (map simplify-table tabs)))))

(defn represent-table-producer-ws "Attempt to represent the tables in this working set." [ws]
  (let [vtables (ws-vtables ws)]
     (for [vt (ws-vtables ws)]
       (make-layer (str "Representation: " (.getName vt)) (rep-table vt)))))

(defn populate-producer-table []
  (add-ws-producer :select-diagrams 
                   "Diagram selection producer" 
                   "Select images with included overlay elements"
                   pv-diagram-selection-producer)
)

(defn populate-layer-table []
    (add-layer-producer :bee-layer
                        "BEE layer creator (layer menu)"
                        "Create a new layer from the current working set."
                        bee-layer-producer-ws)
    (add-layer-producer :text-stats
                        "Text Stats Layer"
                        "Create a new layer providing stats for text characteristics."
                        text-stats-producer-ws)
#_    (add-layer-producer :gene-names
                         "Gene Names Layer"
                        "Create a new layer providing gene names found in the Working Set."
                        gene-layer-producer-ws)
    (add-layer-producer :table
                        "Labeled Table Layer"
                        "Create a table from tagged working sets within this working set."
                        manual-table-producer-ws)
    (add-layer-producer :simple-table
                        "Simplified Label Table Layer"
                        "Create a simplified table (no visual elements) from tagged working sets within this working set."
                        simple-table-producer-ws)
    (add-layer-producer :table-rep 
                        "Table Representation Layer"
                        "Attempt to represent elements of the table."
                        represent-table-producer-ws)
  )
 