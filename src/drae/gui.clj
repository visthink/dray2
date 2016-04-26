(ns drae.gui 
  "Routines for accessing DRAE GUI elements from Clojure.The main GUI routines 
   are in Java portion of the code, but these routines make it easy for 
   Clojure code to pull out key facts from the current GUI. Assumes a single
   GUI instance.

   This package also includes the routines for several default DRAE toys."
  (:import (com.leidos.bmech.gui ViewerApp))
  (:require [clojure.java.io :refer [file]]
            [clojure.data.json :as json]
            [seesaw.core :refer [frame label show! visible!]]
            [seesaw.invoke :refer [invoke-now]]
            [seesaw.chooser :refer [choose-file]]
            [drae.util :refer [cache-directory]]
            [drae.util.inspect :refer [inspect]]
            [drae.wset :refer [ws-images ws-pdf]]
            [drae.j.Toys :refer [add-ws-toy add-sel-toy]]
            [drae.charts :refer [vels->dataset wset->dataset
                                 text-line-edge-scatter 
                                 text-histogram]]
            [drae.ext.bee :refer [bee-rep-for]]
            [drae.paxll :refer [pc2query model! model]]
            [drae.vtable :refer [ws-vtables]]
            ))

(defonce ^{:private true} +gui+ (atom nil)) ;; Current GUI instance.

(defn current-gui "Current GUI" [] @+gui+)

(defn gui-selected-items 
  "Current set of selected items in the DRAE GUI."
  ([gui] (.getSelected (.getView gui)))
  ([] (gui-selected-items (current-gui))))

(defn gui-working-set
  "The current working set in the DRAE GUI."
  ([gui] (.getCurrentWS (.getView gui)))
  ([] (gui-working-set (current-gui))))

(defn gui-vtables 
  "Returns the set of vtables in the currently selected working set."
  []
  (ws-vtables (gui-working-set)))



(defn append-to-log "Append message to current GUI log panel." [msg] 
  (when @+gui+ (.appendToLog @+gui+ msg)))

(defn toy-run-bee "Run BEE on the current working set." [ws]
  (let [vimage (first (ws-images ws))
        pdf-file (ws-pdf ws)
        image-filename (.bitmap-path vimage)]
    (println (format "Running BEE on %s.\n  pdf: %s\n  image-filename: %s.\n" ws pdf-file image-filename))
    (inspect (bee-rep-for pdf-file image-filename)
             :title (str "BEE Output: " image-filename))
    ))

(defn toy-show-image "Show the first image in the working set in a frame." [ws]
  (let [vimage (first (ws-images ws))
        drae-cache (cache-directory (ws-pdf ws))
        image-file (file drae-cache (.bitmap-path vimage))
        bbox (.bbox vimage)]
    (-> (frame 
          :height (* 2 (.getHeight bbox))
          :width (* 2 (.getWidth bbox))
          :title (str image-file)
          :content (label :icon image-file))
      show!)))
       
(defn toy-query-paxtools "Show the value of paxtools query." [items]
  (let [text-strings (distinct (map #(.text %) items))]
    (inspect (pc2query text-strings)
      :title (format "Pathway Commons Query on %s." (into [] text-strings)))))


(defn toy-load-biopax-full 
  "Pre-load the full BioPax example model."
  [_] 
  (append-to-log "Pre-loading BioPax full model...")
  (future 
    (do 
      (model :biopax-full)
      (append-to-log "...Done loading BioPax full model.")
      (model! :biopax-full)))
  )

(defn populate-toys-table 
  
  "Populate the Toys table with the current set of available toys."
  []
  
  (add-sel-toy "Text Line Edge Scatter (Vels)" (comp text-line-edge-scatter vels->dataset))
  
  (add-ws-toy "Text Line Edge Scatter (WS)"    (comp text-line-edge-scatter wset->dataset))
  
  (add-ws-toy "Text Height Histogram (WS)"     (comp (partial text-histogram :height) wset->dataset))
  
  (add-ws-toy "Text Width Histogram (WS)"      (comp (partial text-histogram :width) wset->dataset))
              
  (add-ws-toy "Text LHS Histogram (WS)"        (comp (partial text-histogram :lhs) wset->dataset))
  
  (add-ws-toy "Text RHS Histogram (WS)"        (comp (partial text-histogram :rhs) wset->dataset))
  
  (add-ws-toy "Font size histogram (WS)"       (comp (partial text-histogram :font-size) wset->dataset))
  
  
  ;; (add-ws-toy "EX1: Text Line Histogram (WS)" #(comp text-line-histogram wset->dataset))
        
  (add-sel-toy "Query Pathway Commons (Vels)" toy-query-paxtools)
  
  (add-ws-toy "Run BEE on Working Set image (first only)" toy-run-bee)
  
  (add-ws-toy "Pop-up inset image" toy-show-image)
  
  (add-ws-toy "Load large BioPax model" toy-load-biopax-full)
          
  )

;;; RUN GUI

(defn run-gui "Run the DRAE GUI." 
  ([argstrings]
    (let #_[gui (ViewerApp.)] 
      [gui (ViewerApp/startDrae (into-array String argstrings))]
    #_ (.setVisible (.frame gui) true)
      (swap! +gui+ (fn [_] gui))
      gui))
  ([] (run-gui '())))

