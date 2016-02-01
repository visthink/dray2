;;;;
;;;; Name:      drae.core
;;;; Purpose:   Central routines for DRAE analyzier
;;;;
;;;; Created:   2014-04-01
;;;; Updated:   2014-12-08
;;;;
;;;;
;;;; Main DRAE routines.
;;;;
(ns drae.core 
  "*Main DRAE functions.*"
  (:import (java.io File))
  (:require [drae.util :refer [delete-current-cache-dirs]]
            [drae.gui :refer [run-gui populate-toys-table append-to-log]]
            [drae.producers :refer [populate-producer-table populate-layer-table]] ;; Just to ensure class creation.
            [drae.corpus :refer [corpus]]
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
  #_(println "Shutting down DRAE.")
  #_(println "Emptying cache...")
  (delete-current-cache-dirs)
  #_(println "   ...finished."))

(defn -main 
  "Main DRAE function. Runs GUI."
  [& args]
  (println "MAIN FUNCTION: Args " args)
  (.addShutdownHook (Runtime/getRuntime) 
    (Thread. shutdown-tasks))
  (let [gui (run-gui args)]
    (populate-gui-tables)
    gui))

(def main -main)
