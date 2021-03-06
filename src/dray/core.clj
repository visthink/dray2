;;;;
;;;; Name:      dray.core
;;;; Purpose:   Central routines for DRAY analyzier
;;;;
;;;; Created:   2014-04-01
;;;; Updated:   2014-12-08
;;;;
;;;;
;;;; Main DRAY routines.
;;;;
(ns dray.core 
  "*Main DRAY functions.*"
  (:import (java.io File))
  (:require [seesaw.chooser :refer [choose-file]]
            [dray.util :refer [delete-current-cache-dirs]]
            [dray.gui :refer [run-gui populate-toys-table append-to-log]]
            [dray.producers :refer [populate-producer-table populate-layer-table]] ;; Just to ensure class creation.
            [dray.corpus :refer [corpus]]
            )
  (:gen-class))
    
;;; MAIN FUNCTION (Test for now)

(defn- populate-gui-tables
  "Populate the GUI tables for producers, toys, and similar function calls.
   This is done at runtime to ensure that all the applicable Clojure classes
   are available. Currently just handles the producers table."
  []
  (populate-producer-table)
  (populate-layer-table)
  (populate-toys-table))

(defn- shutdown-tasks
  "This function is called automatically when the JVM exits, and removes cache files
   created while the program was running."
  []
  (delete-current-cache-dirs))
  
(defn -main 
  "Main DRAY function. Runs GUI."
  [& args]
  (println "MAIN FUNCTION: Args " args)
  (.addShutdownHook (Runtime/getRuntime) 
    (Thread. shutdown-tasks))
  (let [gui (run-gui args)]
    (populate-gui-tables)
    gui))

(def main -main)
