(ns drae.config
  "*Configuration information for DRAE. Most of the configuration information for DRAE is handled
   through the drae configuration files `drae-default-settings.edn` and `drae-settings.edn`.*"
  (:import (java.io File))
  (:require [clojure.java.io :refer [file]]
            [clojure.edn :as edn]
            [drae.util :refer [uerr]]
            [drae.util.exec :refer [home-dir unix-which]]
            ))

(defn- drae-settings-files 
  "Returns a list of all the applicable drae-settings and drae-default-settings files,
   in order of increasing precedence (latest is best)."
  []
  (filter  #(.exists %)
          (list (file "./drae-default-settings.edn")
                (file "./drae-settings.edn")
                (file (home-dir) "drae-settings.edn")
                )))

(defn- drae-settings-current
  "Returns a map of all the drae settings, based on the 
   current settings retrieved from the set of drae settings files."
  []
  (apply merge (map #(-> % slurp edn/read-string) (drae-settings-files))))

(def drae-settings 
  "Returns a map of all the drae settings, based on the settings retrieved 
   from the set of drae settings files. Memoized." 
  (memoize drae-settings-current))

(defn drae-setting 
  "Return the given drae setting for the given parameter. Settings
   are read once when any setting is requested, so if the setting files
   are changed, the JVM must be restarted to see an effect. The settings
   are found either in the defalt settings in the project directory 
   (`drae-default-settings.edn`) or in the overriding settings provided by the
   user in the project path or user's home directory (`drae-settings.edn`).

   Available settings are: _:python-executable_ (for running python scripts),
   _:pdftoxml-executable_ (for pulling out the initial set of visual elements),
   _:bee-executable_ (for blob analysis in pathway diagrams), 
   or _:corpora_ (a description of the available corpora). See the 
   `drae-default-settings.edn` file for the format of the corpus descriptions.

   In addition, there are planned settings for _:additional-executable-paths_
   (paths to check for executables when they are not explicitly set)."
  [k]
  {:pre [(keyword? k)]}
  (get (drae-settings) k))

(defn- validate-file
  "Simple validation of executable or other file to assure that it exists in the current system.
   Signals error if missing. Returns true if valid."
  [system-name filename]
  (if-not (.exists (file filename))
    (uerr "Expected path for %s executable %s does not exist." system-name filename)
    true))

#_(defn validate-config 
   "Attempt to validate the python, pdftoxml, and bee executables in the configuration file 
   by checking to make sure sure that they exist in the current file system. Will 
   signal an error if they are not found."
   []
   (validate-file "python" (python-exe))
   (validate-file "pdftxml" (p2x-exe))
   (validate-file "beelibtester" (bee-exe))
   )


