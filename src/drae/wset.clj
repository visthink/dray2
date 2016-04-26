(ns drae.wset
  "Working set routines. Working sets are not created in isolation, so there is no constructor, but 
   are created as children of existing working sets."
  (:import (java.util ArrayList)
           (drae.j.VisualElement VImage)
           (com.leidos.bmech.model WorkingSet))
  (:require [clojure.java.io :refer [file]]
            [clojure.data.json :as json]
            [drae.util :refer [uerr instances]]
            [drae.util.map :refer [remove-null-keys]]
            )
  )

;;; HELPER FUNCTIONS

(defn- empty-bbox? "True if BBox has zero height and width." [b]
  (and (zero? (.getHeight b)) (zero? (.getWidth b))))

;;; WORKING SETS

(defn ws-make-child 
  "Create a child instance of this working set with the given name and bounding box."
  [ws name bbox & {:keys [tags] :or {tags '[]}}]
  (let [string-name (if (keyword? name) (.getName name) name)
        name-keyword (if (string? name) (keyword name) name)]
    (doto (.createChild ws string-name bbox)
      (.setTags (ArrayList. (cons string-name tags))))))

(defn ws-pdf "PDF file linked to this working set." [ws] (file (.getFilename ws)))

(defn ws-items "Items in the working set." [ws] (.getItems ws))

(defn ws-images 
  "Return all VImages in this working set." 
  [ws] 
  (instances VImage (.getItems ws)))

(defn ws-parent "Parent working set for this one (if any)." [ws] (.getParent ws))

(defn ws-root? "Is this a root working set?" [ws] (nil? (ws-parent ws)))
                                                         
(defn ws-root "Top-level working set for this working set." [ws]
  (if (ws-root? ws) ws (ws-root (ws-parent ws))))

(defn ws-leaf? "Is this a working set leaf?" [ws] (empty? (.getChildren ws)))

(defn ws-page? "Is this a page-level working set?" [ws]
  (and (not (ws-root? ws)) (ws-root? (ws-parent ws)))) ; I'm not a root, but my parent is.

(defn ws-page-root
  "The working set for the given page. The ws argument can be any working set in the tree."
  [ws page-no]
  (some #(if (= page-no (.getPage %)) %) (.getChildren (ws-root ws))))

(defn- ws-has-tag? [ws tag]
  (some #(= % tag) (.getTags ws)))

(defn ws-children
  "All the child working sets of this one. If the :tag keyword is used, only 
   returns those descendants with the given tag."
  [ws & {:keys [tag] :or {tag nil}}]
  (let [child-wsets (.getChildren ws)]
    (if tag
      (filter #(ws-has-tag? % tag) child-wsets)
      child-wsets)))

(defn ws-descendants 
  "All the descendant working sets of this one. If the :tag keyword is used, only
   returns those descendants with the given tag."
  [ws & {:keys [tag] :or {tag nil}}]
  (let [child-fn #(.getChildren %)
        all-desc (tree-seq child-fn child-fn ws)]
    (if tag 
      (filter #(ws-has-tag? % tag) all-desc)
      all-desc)))
      
(defn ws-replace-child
  "Replace the old child working set with the new one in the 
   parent working set's children."
  [parent-ws old-child-ws new-child-ws]
  (.setChildren parent-ws 
    (replace {old-child-ws new-child-ws} (.getChildren parent-ws))))
  
(defn ws->map "Write working set as a re-readable map structure." [ws]
  (let [bbox (.getBbox ws)
        children (.getChildren ws)
        assoc-if (fn [m bool k v] (if bool (assoc m k v) m))]
    (remove-null-keys
      {:name (.getName ws)
       :type (when (ws-root? ws) 'working-set)
       :bbox (when-not (empty-bbox? bbox)
               {:x (.getX bbox) :y (.getY bbox) 
                :width (.getWidth bbox) :height (.getHeight bbox)})
       :page (when (ws-page? ws) (.getPage ws))
       :filename (when (ws-root? ws) (.getName (file (.getFilename ws)))) ; Just filename, no path.
       :tags (.getTags ws)
       :children (when-not (empty? children) (map ws->map children))
       }
      )))

(defn ws->json "Write working set as re-readable JSON string. 
  Returns a working set for the second argument, which must
   be a document-level working set."
  [ws] 
  (with-out-str (json/pprint (ws->map ws))))




