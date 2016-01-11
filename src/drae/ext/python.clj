(ns drae.ext.python
  "Routines for running external Python programs."
  (:require [clojure.java.shell :refer [sh with-sh-dir]]
            [drae.config :refer [drae-setting]]
            [drae.util.exec :refer [unix-which]]
            [drae.util :refer [uerr cache-file-for]]
            )
  )

(def ^:dynamic *python-script-dir* "./src/associateBlobsAndText")

(defn python-exe 
  "Return a string path to the python executable, and signal an error if not found."
  []
  (let [setting (drae-setting :python-executable)]
    (if-not (empty? setting)
      setting
      (let [search-res (unix-which "python")]
        (if (empty? search-res) ;; Didn't find.
          (uerr "Could not find executable path for python. Set in drae-settings.edn or add to path.")
          search-res)))))


;;; PYTHON CONNECT
;;; ----------------------------------------------------------------------

(defn run-python 
  [& args]
  (print "Running Python on: " args)
  (with-sh-dir *python-script-dir*
    (let [res (apply sh (python-exe) args)]
      (when-not (zero? (:exit res))
        (uerr "%s" (:err res)))
      res)))

