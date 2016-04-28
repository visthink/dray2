(ns dray.j.Paxll
  "A class querying the Pathway Commons 2 interface."
  (:import  (java.util HashMap)
            (org.biopax.paxtools.model Model))
  (:require [dray.paxll :refer [pc2query model! names-index find-name-class find-bioentity]])
  (:gen-class :name dray.j.Paxll
              :state state
              :constructors {[] []} 
              :prefix "-"
              :methods  [ ^{:static true} [query [java.lang.String] java.util.HashMap] 
                          ^{:static true} [setModel [java.io.File] org.biopax.paxtools.model.Model]
                          ^{:static true} [namesIndex [] java.util.HashMap]
                          ^{:static true} [nameClasses [java.lang.String] java.util.List]
                          ^{:static true} [nameBioentities [java.lang.String] java.util.List]
                         ]
              ))

(defn query 
  "Returns a HashMap of querying on the given name. If there are 
   multiple names divided by slashes, will return multiple values."
  [^String entity-name] (java.util.HashMap. (pc2query entity-name)))

(def -query "Java equivalent" query) 

(defn -setModel 
  "Sets the current model when passed a BioPax owl filename. Returns model."
  [f]
  (model! f))

(def setModel -setModel)

(defn namesIndex 
  "For the current BioPax model, creates and returns a names index that can be used to determine the potential
   types of named entities. Returns a HashMap where the keys are keywords (e.g., :Protein) and the value is 
   a HashSet of all the name strings."
  [] (names-index))

(def -namesIndex namesIndex)

(defn nameClasses 
  "Returns a list of bioentity classes for the given name in the current BioPax model, as
   a list of strings."
  [namestring]
  (into [] (map #(.getName %) (find-name-class namestring))))

(def -nameClasses nameClasses)

(defn nameBioentities 
  "Returns a list of BioPax bioentity objects for the given name in the current BioPax model."
  [namestring]
  (into [] (find-bioentity namestring)))

(def -nameBioentities nameBioentities)
