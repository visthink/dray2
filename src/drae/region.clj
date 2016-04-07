(ns drae.region
  (:import (drae.j.VisualElement VDocument)
           )
  (:require [clojure.java.io :refer [file]]
            [clojure.walk :refer [keywordize-keys stringify-keys]]
            [clojure.data.json :as json]
            [drae.util :refer [uerr]]
            [drae.doc :refer [make-bbox]]
            [drae.data :refer :all]
            )
  )

(defn region? [x] (instance? drae.data.Region x))

(defn make-region 
  "Create a new region object."
  [parent page boundary 
   & {:keys [name tags level] :or {name (gensym "Region") 
                                   tags []
                                   level 0}}]
  (map->Region {:name name
                :children '[]
                :page page
                :boundary boundary
                :level level
                :microkb nil
                :tags tags
                }))

(defn modified-region 
  "Apply the given changes and return a new region."
  [r & {:keys [children boundary tags] :as changes}]
  (map->Region (merge r changes)))

(defn add-child [r child]
  (modified-region r :children (cons child (.children r))))

(defn remove-child [r child]
  (modified-region r :children (remove #(= % child) (.children r))))

               
;;; OVERLAYS


(defn- bbox-map->bbox [m]
  (let [{:keys [x y width height]} m]
    (make-bbox x y width height)))

;;; JSON->OVERLAY

(defmulti json->overlay "Read in either a JSON string or a file as a new working set overlay." class)

(defmethod json->overlay java.lang.String [s]
  (-> s json/read-str keywordize-keys))

(defmethod json->overlay java.io.File [json-file]
  (-> json-file slurp json->overlay))

(defn overlay-map->region [overlay-map & {:keys [level] :or {level 0}}]
  (println "Creating region " (:name overlay-map))
  (map->Region (merge
                 overlay-map ; Take tags, page & name directly from map.
                 {:boundary (if (:bbox overlay-map) (bbox-map->bbox (:bbox overlay-map)))
                  :level level
                  :microkb nil
                  :children 
                    (mapv #(overlay-map->region % :level (inc level)) (:children overlay-map))
                 }
               ))
  )
                  
                  
                
  
