(ns dray.xml
  "A set of utility routines for dealing with XML, CLJXML (the format produced
   by clojure.data.xml) and prxml (which is a more compact XML format produced
   by Clojure."
  (:import (org.apache.xml.serialize OutputFormat XMLSerializer))
  (:require [clojure.java.io :refer [file writer]]
            [clojure.data.xml :refer [indent-str sexp-as-element]]
            [clojure.zip :refer [zipper]]))

;;; SERIALIZE XML

(defn serialize-xml 
  "Given an ImplDoc and a filename, serialize the XML to the file."
  [impl-doc filename]
  (with-open [out (writer (file filename))]
    (let [out-format (OutputFormat. impl-doc "UTF-8" true)
          serializer (XMLSerializer. out out-format)]
      (.serialize serializer impl-doc)
      (.flush out)))
  filename)

;;; CLJXML

(defn cljxml-node? [x] (and (map? x) (not (nil? (:tag x)))))

(defn cljxml->prxml [x]
  (if-not (cljxml-node? x)
    x
    (let [{:keys [tag attrs content]} x]
      `[~tag ~@(if (empty? attrs) () (list attrs))
        ~@(map cljxml->prxml content)])))

;;; PRXML

(def prxml-tag first)

(defn prxml-attrs [prxml-exp] 
  (if-not (map? (second prxml-exp)) '() (second prxml-exp)))

(defn prxml-attr "Retrieves the value of a particular PRXML expresion."
  [prxml-exp attr]
  (get (prxml-attrs prxml-exp) attr))

(defn prxml-content [prxml-exp] 
  (if (empty? (prxml-attrs prxml-exp)) 
    (rest prxml-exp)
    (rest (rest prxml-exp))))

(defn prxml-node? [x] 
  (and (vector? x) (keyword? (first x)) (map? (second x))))

(defn prxml-replace-content [node new-content]
  (let [attrs (prxml-attrs node)]
    (if-not (empty? attrs)
      `[~(prxml-tag node) ~attrs ~@new-content]
      `[~(prxml-tag node) ~@new-content])))
  
(defn prxml->xml
  "Given an expression in prxml format, returns the equivalent XML as a 
   string. Performs indentation of XML expressions as needed."
  [prxml-exp]
  (indent-str (sexp-as-element prxml-exp)))

(defn prxml-zipper 
  "Given a PRXML structure, returns a zipper on that structure."
  [x]
  (zipper prxml-node? 
          prxml-content ;; The content args of the prxml expression
          prxml-replace-content
          x))

 