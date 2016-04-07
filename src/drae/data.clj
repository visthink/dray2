(ns drae.data
  "A region of a document. Can contain subregions and representations."
  (:import (java.util ArrayList)
           (drae.j.VisualElement VDocument)
           )
  (:require [clojure.java.io :refer [file]]
            [drae.util :refer [uerr]]
            [drae.doc :refer [get-vdocument]]
            )
  )

(defprotocol Tree 
  )

(defrecord Region 
    [name        ;; Name for the region
     parent      ;; Parent region that contains this one. 
     children    ;; Contained 'child' regions of this one.
     level       ;; Number of levels down (page level = 0)
     page        ;; Page this is contained on.
     boundary    ;; Boundary - Rectangle or other shape.
     microkb     ;; Tiny fact set for reasoning, etc.
     tags        ;; High-level category labels.
     ]
  )


(defrecord Interpreter 
     [file        ;; Current PDF file.
      document    ;; Its VDocument object.
      regions     ;; Page-level regions (mutable via atom)
      history     ;; History of past region sets, also mutable.
      kb          ;; Top-level KB
      time        ;; Time for timestamp
     ]
   Object
     (toString [this]
       (format "<Interpreter: %s>" 
               (.toString (or (.file this) (.getFilename (.document this)) "nil"))))
     )

