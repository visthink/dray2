(ns drae.interpreter
  (:import (drae.j.VisualElement VDocument)
           (drae.data Interpreter Region)
           )
  (:require [clojure.java.io :refer [file]]
            [drae.util :refer [uerr]]
            [drae.data :refer :all]
            [drae.doc :refer [get-vdocument]]
            )
  )



;;; Interpreter Constructors
;;; --------------------------------------------

(defmulti make-interpreter "Return a new Interpretor object for a PDF, filename, or VDocument instance." class)

(defmethod make-interpreter :default [x]
  (uerr "Cannot make an Interpretor object from argument: %s" x))

(defmethod make-interpreter VDocument [vdoc]
   (map->Interpreter {:document vdoc
                      :file (.getFilename vdoc)
                      :regions '[]
                      :kb nil
                      :time 0
                      :history '[]
                      }
                     ))

(defmethod make-interpreter java.io.File [pdf]
  (make-interpreter (get-vdocument pdf)))
  
(defmethod make-interpreter String [pdf-filename]
  (make-interpreter (file pdf-filename)))

(def interpreter-history-length 20)

(defn modified-interpreter 
  "Apply the given map to the given Interpreter to produce a modified Interpreter."
  [i & {:keys [regions kb] :as change-map}]
  (map->Interpreter 
    (merge i change-map
           {:history (take interpreter-history-length (cons i (.history i)))
            :time (inc (.time i))}
           )))

;;; Handling Region overlays.
;;; --------------------------------------------

(defn add-overlay [interpreter overlay]
  (println "Adding overlay")
  
  )

