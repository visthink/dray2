(ns drae.j.Doc
  "Definitions for the Document and various vel (visual element) types. These types follow the
   PDF element types fairly closely, and so contain more style information than 
   the original vels. Also, they should be mappable to the Java Shape interface.
   For now the PDFTOXML input filter is also part of this namespace, though
   this may change."
  (:require [clojure.java.io :refer [file]]
            [drae.ext.ghostscript :refer [use-ghostscript? generate-page-images page-image-file page-image-for]]
            [drae.doc :refer [get-vdocument]]
            )
  (:gen-class :name drae.j.Doc
              :state state
              :constructors {[] []}
              :prefix "-"
              :methods [^{:static true} [getVDocument [java.io.File] java.lang.Object]
                        ^{:static true} [useGhostscript [] java.lang.Boolean]
                        ^{:static true} [generatePageImages [java.io.File] java.lang.Integer]
                        ^{:static true} [getPageImageFile [java.io.File java.lang.Integer] java.io.File]
                        ^{:static true} [saveWStoOverlay [java.lang.Object java.io.File] java.lang.Object]
                        ^{:static true} [restoreWSfromOverlay [java.lang.Object java.io.File] java.lang.Object]
                        ^{:static true} [getPageImageFor [java.io.File java.lang.Integer] java.io.File]
                        ])
  )


(def -getVDocument "Java method for get-vdocument." get-vdocument)

;;; GHOSTSCRIPT PAGE IMAGES
;;; -------------------------------------------------------------------------

#_(defn use-ghostscript 
   "Returns true if the configuration indicates that Ghostscript should be used."
   []
   (use-ghostscript?))

(def -useGhostscript "Java method for `use-ghostscript`." use-ghostscript?)

(def -generatePageImages "Java method for `generate-page-images`." generate-page-images)

(defn get-page-image-file
  "Given a pdf file, returns the corresponding image file for that page."
  [pdf-file n]
  (page-image-file pdf-file n))

(def -getPageImageFile "Java method for `get-page-image-file`." get-page-image-file)

(def -getPageImageFor "Java method for `page-image-for`." page-image-for)

;;; WORKING SET OVERLAYS
;;; -------------------------------------------------------------------------

;; We forward-reference the save and restore functions (below) because we cannot precompile them
;;  at this point in the compilation proces.

(defn -saveWStoOverlay 
  "Java method for `dm-save-ws-to-overlay`." 
  [dm json] 
  (if-let  [save-fn (find-var 'drae.manager/dm-save-ws-to-overlay)]
    (save-fn dm json)
    (println "Cannot find overlay save function.")))
      
(defn -restoreWSfromOverlay 
  "Java method for `dm-restore-ws-to-overlay`." 
  [dm json-file]
  (if-let [restore-fn (find-var 'drae.manager/dm-restore-ws-from-overlay)]
    (restore-fn dm json-file)
    (println "Cannot find overlay restore function.")))


