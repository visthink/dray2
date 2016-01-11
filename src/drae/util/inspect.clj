(ns drae.util.inspect
  "Augmented version of the Clojure inspector."
  (:require [clojure.inspector :as insp]
            [seesaw.core :refer [frame label show! config!]]
            [drae.util :refer [uerr]])
  )

(defn- munge-data "Munge Java types into something Clojure inspector can recognize." [x]
  (cond (instance? java.util.ArrayList x) (into [] x)
        :else x))
 
(defn inspect 
  "Create a new inspector for the given object. Optional title (string),
   and inspector-type (:tree or :table). Some Java-only collections may
   be coerced into Clojure versions that can be more easily displayed."
  [x & {:keys [title inspector-type] 
        :or {title (format "%s %s" (.getSimpleName (class x)) x)
             inspector-type :tree}}]
   (let [munged-x (munge-data x)
         i (case inspector-type 
             :tree (insp/inspect-tree munged-x) 
             :table (insp/inspect-table munged-x)
             (uerr "Bad inspector type: %s" inspector-type))]
     (doto i (config! :title title))))


