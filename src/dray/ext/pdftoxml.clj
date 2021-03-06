(ns dray.ext.pdftoxml
  "Routines for running PDFtoXML."
  (:require 
    [clojure.java.shell :refer [sh]]
    [dray.util :refer [cache-file-for uerr ->filename]]
    [dray.util.exec :refer [unix-which]]
    [dray.config :refer [dray-setting]]
    ))

(defn p2x-exe
  "Return a string path to the pdftoxml executable, and signal an error if not found."
  []
  (let [setting (dray-setting :pdftoxml-executable)]
    (if-not (empty? setting)
      setting
      (let [search-res (unix-which "pdftoxml")]
        (if (empty? search-res) ;; Didn't find.
          (uerr "Could not find executable path for pdftoxml. Set in dray-settings.edn or add to path.")
          search-res)))))

;;; PDFTOXML
;;; ----------------------------------------------------------------------

(defn pdftoxml-file-for [pdf] (cache-file-for "p2x-" ".xml" pdf))

(defn pdftoxml-output 
  "Returns the pdftoxml file result for the given pdf, 
   running pdftoxml if needed."
  [pdf]
  (let [f (pdftoxml-file-for pdf)
        sh-result (sh (p2x-exe) (->filename pdf) (->filename f))]
    (when-not (zero? (:exit sh-result))
      (uerr "PDFtoXML exited with error code: %s. \nMessage: %s" 
            (:exit sh-result) (:err sh-result)))
    f))

