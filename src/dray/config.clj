(ns dray.config
  "*Configuration information for DRAY. Most of the configuration information for DRAY is handled
   through the dray configuration files `dray-default-settings.edn` and `dray-settings.edn`.*"
  (:import (java.io File))
  (:require [clojure.java.io :refer [file]]
            [clojure.edn :as edn]
            [dray.util :refer [uerr]]
            [dray.util.exec :refer [home-dir unix-which]]
            ))

(defn- dray-settings-files 
  "Returns a list of all the applicable dray-settings and dray-default-settings files,
   in order of increasing precedence (latest is best)."
  []
  (filter  #(.exists %)
          (list (file "./dray-default-settings.edn")
                (file "./dray-settings.edn")
                (file (home-dir) "dray-settings.edn")
                )))

(defn- dray-settings-current
  "Returns a map of all the dray settings, based on the 
   current settings retrieved from the set of dray settings files."
  []
  (apply merge (map #(-> % slurp edn/read-string) (dray-settings-files))))

(def dray-settings 
  "Returns a map of all the dray settings, based on the settings retrieved 
   from the set of dray settings files. Memoized." 
  (memoize dray-settings-current))

(defn dray-setting 
  "Return the given dray setting for the given parameter. Settings
   are read once when any setting is requested, so if the setting files
   are changed, the JVM must be restarted to see an effect. The settings
   are found either in the defalt settings in the project directory 
   (`dray-default-settings.edn`) or in the overriding settings provided by the
   user in the project path or user's home directory (`dray-settings.edn`).

   Available settings are: _:python-executable_ (for running python scripts),
   _:pdftoxml-executable_ (for pulling out the initial set of visual elements),
   _:bee-executable_ (for blob analysis in pathway diagrams), 
   or _:corpora_ (a description of the available corpora). See the 
   `dray-default-settings.edn` file for the format of the corpus descriptions.

   In addition, there are planned settings for _:additional-executable-paths_
   (paths to check for executables when they are not explicitly set)."
  [k]
  {:pre [(keyword? k)]}
  (get (dray-settings) k))

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


