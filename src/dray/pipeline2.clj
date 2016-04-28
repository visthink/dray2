;;;
;;; THIS CODE IS NO LONGER USED. 4/2015. RWF
;;;
;;;
;;;
(ns dray.pipeline2
  "*NO LONGER USED*
   This covers the routines for the pdftoxml-based pipeline, which uses pdftoxml along with a set
   of Python routines and blob output to generate an XML description of the blobs and labels."
  (:import (java.io File)
           (java.lang.reflect Field)
           (javax.swing JComponent)
           )
  (:require [clojure.java.shell :refer [with-sh-dir]]
            [clojure.java.io :refer [file]]
            [clojure.data.xml :refer [parse-str]]
            [dray.util :refer [cache-file-for ->filename uerr]]
            [dray.xml :refer [prxml->xml cljxml->prxml prxml-tag prxml-attrs prxml-content]]
            [dray.ext.ghostscript :refer [page-image-file]]
            [dray.ext.python :refer [run-python *python-script-dir*]]
            [dray.ext.pdftoxml :refer [pdftoxml-file-for pdftoxml-output]]
            
            )
  )

;;; OVERLAYS (TextExtractorXML)
;;; ----------------------------------------------------------------------

(defn- overlay-file-for [pdf] (cache-file-for "overlays-" ".xml" pdf))

(defn- overlay-output [pdf]
  (let [p2x-file (pdftoxml-file-for pdf)
        overlay-file (overlay-file-for pdf)]
    (when-not (.exists p2x-file) (pdftoxml-output pdf))
    (with-sh-dir *python-script-dir*
      (run-python "TextExtractorXML.py" (.getCanonicalPath p2x-file) (.getCanonicalPath overlay-file)))
    overlay-file))

(defn overlay-prxml [pdf]
  (-> pdf overlay-output slurp parse-str cljxml->prxml))
    
(defn page-image-prxml [pdf]
  (let [overlay (overlay-prxml pdf)] 
    (if (= :IMAGE_LIST (prxml-tag overlay))
      `[:page-image 
        ~(assoc (prxml-attrs overlay)
                :src (->filename (.getName (page-image-file pdf 1))))]
      (uerr "Overlay with page dimensions - incorrect format? %s" overlay))))

(defn page-images-prxml [pdf]
  (let [overlay (overlay-prxml pdf)]
    (when (= :IMAGE_LIST (prxml-tag overlay))
      (for [image-node (prxml-content overlay)]
        `[:image-segment  ~(prxml-attrs image-node)]
        ))))

(defn- first-image-upper-left [images-prxml]
  (let [attrs (prxml-attrs (first images-prxml))]
    [(read-string (:x attrs)) (read-string (:y attrs))]))

(defn translate-attrs-in [node x-diff y-diff scale]
  (let [orig-attrs (prxml-attrs node)
        transform-attr 
        (fn [m k offset scale]
          (if-not (get m k) m ; Skip if no key.
            (update-in m [k] (fn [v off sc] (+ (* sc (read-string v))
                                               (* sc off))) 
                       offset scale)))]
    (if (nil? (:x orig-attrs)) node ;; No x,y - skip whole thing.
      (let [attrs-changed ;; Otherwise, rewrite box
            (-> orig-attrs
              (transform-attr :x x-diff scale)
              (transform-attr :y y-diff scale)
              (transform-attr :h 0.0 scale)
              (transform-attr :w 0.0 scale)
              (transform-attr :font-size 0.0 scale)
              )]
      (if-not (empty? (prxml-content node))
        `[~(prxml-tag node) ~attrs-changed ~@(prxml-content node)]
        `[~(prxml-tag node) ~attrs-changed])))))

(defn translate-attrs-in-node-list [node-list x-diff y-diff scale]
  (map #(translate-attrs-in % x-diff y-diff scale) node-list))


;;; BLOB FILE 
;;; ----------------------------------------------------------------------

#_(defn blob-file-for [pdf image-filename]
    #_(file (.getParent pdf) "precomp" "blob" (str "blobs_" (rootname pdf) "-000.txt"))
    )

#_(defn image-file-for [pdf] ;; Buggy --assumes one image per file.
   (file (str (.getPath (pdftoxml-file-for pdf)) "_data" "/image-1.jpg")))

;;; Association
;;; ----------------------------------------------------------------------

#_(defn associations-file-for [pdf] (cache-file-for "assocs-" ".xml" pdf))

#_(defn rewrite-xml-file 
   "Rewrite an XML file by reading it in, transforming it
   to PRXML, apply the xform-fn, and then writing it back as XML.
   If output-file not provide, writes result back to input file."
   ([xml-file xform-fn output-file]
     (spit output-file 
        (-> xml-file ; pipeline from file -> prxml -> xform -> xml
          slurp parse-str cljxml->prxml xform-fn prxml->xml))))

#_(defn rewrite-assocs-file [f]
   (let [x (cljxml->prxml (parse-str (slurp f)))]
     (spit f 
       (prxml->xml 
         `[PDFResult 
           [page {:page_number 1}
            ~@(rest x)]]))))

#_(defn assocs-output [pdf]
   (let [overlay-file (overlay-file-for pdf)
         blob-file (blob-file-for pdf)
         assoc-file (associations-file-for pdf)
         fpath #(.getCanonicalPath %)]
     (when-not (.exists blob-file) (uerr "Cannot find blob file: %s" blob-file))
     (when-not (.exists overlay-file) (overlay-output pdf))
     (apply run-python "associateBlobsWithText.py" 
            (map fpath (list blob-file overlay-file assoc-file)))
     assoc-file))
          
;;; COMBINED

#_(defn combined-file-for [pdf] (cache-file-for "combined-" ".xml" pdf))

#_(defn combined-output [pdf] 
   (let [page-image-prxml (page-image-prxml pdf)
         page-images-prxml (page-images-prxml pdf)
         assocs-prxml (-> pdf assocs-output slurp parse-str cljxml->prxml rest)
         [x-diff y-diff] (first-image-upper-left page-images-prxml)
         rewritten-assocs
         (translate-attrs-in-node-list assocs-prxml x-diff y-diff 2.0)
         rewritten-images
         (translate-attrs-in-node-list page-images-prxml 0.0 0.0 2.0)]
     (into rewritten-assocs (conj rewritten-images page-image-prxml))))

#_(defn combined-output-file [pdf]
   (let [f (combined-file-for pdf)]
     (spit f
           (prxml->xml 
            `[PDFResult
              [page {:page_number 1}
               ~@(combined-output pdf)]]))
     f))

