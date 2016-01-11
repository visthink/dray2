(ns drae.ext.bee
  "Routine for calling Biological Entity Extractor."
  (:import (java.io File)
           (java.nio.file Path))
  (:require [clojure.java.shell :refer [sh with-sh-dir]]
            [clojure.java.io :refer [file]]
            [clojure.string :as s]
            [seesaw.color :refer [color]]
            [drae.util :refer [resolve-cache-file-for rootname make-dirs uerr]]
            [drae.util.map :refer [mapify]]
            [drae.util.exec :refer [unix-which]]
            [drae.doc :refer [make-bbox]]
            [drae.config :refer [drae-setting]]
            )
  )

(defn- bee-exe
  "Return a string path to the bee executable, and signal an error if found."
  []
  (let [setting (drae-setting :bee-executable)]
    (if-not (empty? setting)
      setting
      (let [search-res (unix-which "beelibtester")]
        (if (empty? search-res) ;; Didn't find.
          (uerr "Could not find the executable path for bee. Set in drae-settings.edn or add to path.")
          search-res)))))
 
;;; BEE
;;; ----------------------------------------------------------------------
(defn- bee-full-path 
  "Full path for executable." 
  []
  (.getCanonicalPath (file (bee-exe))))

(defn- starts-with? [file dir-path]
  (.startsWith (.getPath file) (.getPath dir-path)))

(defn- image-full-path
  "Given the PDF file and the image filename (a relative path), 
   return the full image file. If image-filename is already a full path, 
   then just return it."
  [pdf-file image-filename]
  (let [res (resolve-cache-file-for pdf-file (file image-filename))]
    (if (.exists res)
      res
      (let [new-res (file (.getCanonicalPath (file image-filename)))]
        new-res))))

(defn- bee-dir-for
  "Return a (possibly newly-created) directory to hold the results of running
   BEE on the given pdf file. The PDF and image file are used to locate
   the proper path, which is adjacent to the image file. Does not create
   the directory."
  [pdf-file image-filename]
  (let [image-file (image-full-path pdf-file image-filename)
        rootname (rootname image-file)
        image-dir (.getParent image-file)]
    (file image-dir (format "BEE-%s" rootname))))

(defn- bee-output-filename 
  "Returns a string of the filename (not full path)."
  [image-file]
  (str (rootname image-file) "-bee-output.txt"))

(defn- bee-output-file-for [pdf-file image-filename]
  (file (bee-dir-for pdf-file image-filename) 
        (bee-output-filename (file image-filename))))

(defn- add-bbox-to-map
  "Replace :bbox (x1 y1 h w) with :bbox <bbox> instance."
  [m]
  (let [bbox-args (map double (:BoundingBox m))]
    (-> m
      (assoc :bbox (make-bbox bbox-args))
      (dissoc :BoundingBox))))

(defn- add-color-to-map
  "Replace color entries with a single color entries with a length-three vector."
  [m]
  (let [{:keys [MeanBlue MeanRed MeanGreen]} m]
    (-> m 
      (assoc :color (color MeanBlue MeanRed MeanGreen))
      (dissoc :MeanBlue :MeanRed :MeanGreen)))) ; Wait until after GUI update to remove old values.
  
(defn- group-by-blob-groups
  "Group the blob maps by the :GroupIndex value."
  [ml]
  {:pre [(coll? ml) (map? (first ml))]} ; collection of maps
  (group-by :GroupIndex ml))

(defn- mapify-bee-output [bee-list]
  (let [m1 (mapify bee-list)
        m2 (assoc m1 :Blobs (doall (map (comp add-color-to-map add-bbox-to-map mapify) (:Blobs m1))))
        m3 (assoc m2 :blob-groups (group-by-blob-groups (:Blobs m2)))]
    m3))

(defn- read-bee-output 
  [f]
  (mapify-bee-output (clojure.edn/read-string (slurp f))))

;;; For BEE, the arguments are 
;
;beelibtester Diagram_Image_Filename Diagram_Image_Filename_FullPath Output_Blob_Filename isRemoveBG(optional)
;
;   where:
;    - Diagram_Image_Filename is the file name for the diagram image in the local disk
;    - Diagram_Image_Filename_FullPath is the (relative) file path/name for the diagram image to put in the output blob text file
;    - Output_Blob_Filename is the path for the output file
;    - isRemoveBG is 1 to use the optional background removal routine, and 0 otherwise

;; Special note: We replace any backslashes in the Image File path with slashes, 
;;   because otherwise they are included in the filename of BEE's output file, 
;;   and the EDN reader in Clojure treats them as escape characters.

(defn- run-bee-on [pdf-file image-filename]
  (let [bee (bee-full-path)
        output-dir (make-dirs (bee-dir-for pdf-file image-filename))
        image-file (image-full-path pdf-file image-filename)
        new-image-filename (s/replace (.getPath image-file) \\ \/)
        rel-image-file (file ".." (.getName image-file))
        output-filename (bee-output-filename image-file)
        ]
    (println "\n Running BEE on" new-image-filename "...")
    (with-sh-dir output-dir
      (sh bee (str rel-image-file) new-image-filename output-filename "1"))
    (println "\n ...done.")
    ))

(defn bee-rep-for 
  "Given an image file and the PDF file from which it was derived,
   run the BEE system on the image and return a map-based representation
   of the blobs and blob groups."
  [pdf-file image-filename]
  {:pre [(instance? java.io.File pdf-file) (string? image-filename)]}
  (run-bee-on pdf-file image-filename)
  (read-bee-output (bee-output-file-for pdf-file image-filename)))

