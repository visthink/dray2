(ns drae.ext.ghostscript
  "Routine for calling Ghostscript to produce background image. Depracted -- will likely
   be removed in later version because DRAE will use Java-based rendering in the future."
  (:import (java.io File))
  (:require [clojure.java.shell :refer [sh with-sh-env with-sh-dir]]
            [drae.util :refer [cache-directory rootname uerr]]
            [drae.util.exec :refer [add-to-system-path unix-which *additional-exec-paths*]]
            [drae.config :refer [drae-setting]]
            )
  )


;;; GHOSTSCRIPT
;;; ----------------------------------------------------------------------

(defn ghostscript-path 
  "Attempts to find and return the location of the Ghostscript executable.
   See also [[drae.util.exec/*additional-exec-paths*]] and [[drae.config/*gs-args*]]."
  []
  (or (drae-setting :ghostscript-executable)
      (uerr "Could not find path to Ghostscript executable in config file.")))
  
(defn use-ghostscript?
  "True if Ghostscript is to be used (as indicated in the config file)."
  []
  (drae-setting :use-ghostscript)) ; If false or missing, don't use.

(def ^{:dynamic true, :private true}  *gs-args* 
   "Default arguments for calls to Ghostscript."
   ["-dBATCH", "-sDEVICE=jpeg", "-r300" "-dTextAlphaBits=4" "-dAlignToPixels=0" "-dNOPAUSE" 
    "-dDOINTERPOLATE" "-dQUIET" "-dNOPAGEPROMPT" "-q" "-dUseCropBox"])

(defn- gs-output-filestring
   "For the given PDF file, retrieve the cache file directory and create the output file
    pattern for that file."
   [f] 
   (let [rootname (#(subs % 0 (- (count %) 4)) (.getName f))]
     (str (.getCanonicalPath (cache-directory f)) "/" "image-" rootname "-%03d.jpg")))
  
(defn gs-page-output-filestring [pdf page-no]
  (let [root (drae.util/rootname pdf)]
    (format "%s/image-%s-%03d.jpg" (.getCanonicalPath (cache-directory pdf))
            root page-no)))

(defn page-image-file 
  "Returns (as a file) the image file for a particular page."
  [pdf n]
  (let [rootname (rootname pdf)]
    (clojure.java.io/file (cache-directory pdf) 
              (format "image-%s-%03d.jpg" rootname n))))

(defn- make-gs-arglist 
  ([input-file page-no]
    {:pre [(or (nil? page-no) (integer? page-no))]}
    (let [output-filestring 
          (if page-no (gs-page-output-filestring input-file page-no)
            (gs-output-filestring input-file))]
      (concat *gs-args* 
              (when page-no (list (format "-dFirstPage=%d" page-no)))
              (when page-no (list (format "-dLastPage=%d" page-no)))
              [(str "-sOutputFile=" output-filestring #_"'")
               (str #_"'" (.getCanonicalPath input-file) #_ "'")])))
  ([input-file] (make-gs-arglist input-file nil)))
                               
 (defn generate-page-images
   "Create a bitmap from a given page of the given PDF file using Ghostscript.
    Returns the exit code (0 = successful). First and last pages are optional
    entries for first and last page numbers."
   [pdf-file]
   #_(print "Running GENERAGE-PAGE-IMAGES")
   (with-sh-dir (cache-directory pdf-file)
     (:exit ;; Return exit code from shell call.
            (doto (apply sh (ghostscript-path) (make-gs-arglist pdf-file))
              (println)
              ))))
 
 (defn generate-page-image
   "Create a bitmap for a single page of a given PDF file using Ghostscript.
    Returns the exist code (0 = successful)."
   [pdf-file page-no]
   (with-sh-dir (cache-directory pdf-file)
     (:exit ; Return exit code
        (apply sh (ghostscript-path) (make-gs-arglist pdf-file page-no)))))
 
 (defn page-image-for-direct 
   "Runs Ghostscript on a single page and (when complete) returns the 
    image file. Not memoized."
   [pdf n]
   {:pre [(integer? n)]}
   (generate-page-image pdf n) ; Single page.
   (let [image-file (page-image-file pdf n)]
     (if-not (.exists image-file)  ;; Existence check bf return.
       (uerr "No Ghostscript page image generated for pdf %s, page %d. File %s not found."
             pdf n image-file))
     image-file))
 
 (def page-image-for 
   "Runs Ghostscript on a single page and (when complete) returns the 
    image file. Memoized." 
   (memoize page-image-for-direct))
 
   
   