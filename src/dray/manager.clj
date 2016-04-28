(ns dray.manager
  "Support for the DataManager, WorkingSet, Layer and LayerList classes in DRAY.
   Working set functions begin with *ws-*, and data manager functions begin with *dm-*."
  (:import (java.util ArrayList)
           (dray.j.VisualElement VImage VDocument)
           (com.leidos.bmech.model DataManager Layer LayerList WorkingSet))
  (:require [clojure.data.json :as json]
            [clojure.java.io :refer [file]]
            [clojure.walk :refer [keywordize-keys stringify-keys]]
            [dray.j.Doc ]
            [dray.doc :refer [make-bbox bbox->bbox-map]]
            [dray.util :refer [uerr #_instances]]
            #_[dray.util.map :refer [remove-null-keys]]
            [dray.wset :refer [ws-make-child ws-pdf ws-items ws-page-root ws->json]]
            )
  )
;;; DATA MANAGER CONSTRUCTORS

(defmulti make-data-manager "Return a new data manager given either a pdf file, a filename, or a VDocument instance." class)

(defmethod make-data-manager :default [x]
  (uerr "Cannot make a data manager from the argument: %s" x))

(defmethod make-data-manager java.io.File [pdf]
  #_ "Create a new data manager from a PDF file."
  (doto (DataManager.) (.setPdfFile pdf)))

(defmethod make-data-manager java.lang.String [pdf-filename]
  (make-data-manager (file pdf-filename)))

(defmethod make-data-manager dray.j.VisualElement.VDocument [vdoc]
  #_"Create a new data manager from the vdoc"
  (doto (DataManager.) (.loadFromVDocument vdoc)))

(defmethod print-method DataManager [x writer]
  (.write writer (format "<DataManager: %s>" (.toString (or (.getPdfFile x) (.getFilename (.getVDocument x)) "nil")))))


(defn- bbox-map->bbox [m]
  (let [{:keys [x y width height]} m]
    (make-bbox x y width height)))

;;; JSON->OVERLAY

(defmulti json->overlay "Read in either a JSON string or a file as a new working set overlay." class)

(defmethod json->overlay java.lang.String [s]
  (-> s json/read-str keywordize-keys))

(defmethod json->overlay java.io.File [json-file]
  (-> json-file slurp json->overlay))


;; DATA MANAGERS - ADD OVERLAY

(defn- add-subpage-level-overlay [ws overlay]
  ;; Adding an overlay to the given working set, and then iterating.
  (let [{:keys [name bbox tags]} overlay
        sub-ws (ws-make-child ws name (bbox-map->bbox bbox) :tags tags)] ;; New sub-ws.
    ;; Iterate if there are sub-overlays as well (no-op if no children).
    (doseq [sub-overlay (:children overlay)]
      (add-subpage-level-overlay sub-ws sub-overlay))))

(defn- add-page-level-overlay [ws overlay]
  (let [page-ws (ws-page-root ws (:page overlay))]
    #_(println "\n;; Subpage count: " (count (:children overlay)))
    (doseq [subpage-overlay (:children overlay)]
      (add-subpage-level-overlay page-ws subpage-overlay))))

(defn- add-top-level-overlay [ws overlay]
  (doseq [page-overlay (:children overlay)]
    (add-page-level-overlay ws page-overlay)))

(defn dm-add-overlay 
  "Overlay a top-level working set overlay onto the working set in this data manager."
  [dm overlay-map]
  (add-top-level-overlay (.getHeadWorkingSet dm) overlay-map))
    
(defn dm-save-ws-to-overlay 
  "Save given data manager's current working sets as an overlay
   in a JSON file. Json-file can be either a file object or string.
   Returns the data manager."
  [dm json-file]
  (let [filename (if (string? json-file) json-file (.getCanonicalPath json-file))]
    (spit filename (ws->json (.getHeadWorkingSet dm)))
    dm))

(defn- chop-file-extension [f] 
  (let [name (.getName f)]
    (subs name 0 (- (count name) 4))))

(defn dm-restore-ws-from-overlay
  "Restore the set of working sets described in the JSON file.
   Returns the modified data manager object. If the second argument is
   omitted, load the default ws-overlay file if it exists."
  ([dm json-file]
    (let [f (if (string? json-file) (file json-file) json-file)]
      (dm-add-overlay dm (json->overlay f))
      dm))
  ([dm]
    (let [pdf (.getPdfFile dm)]
      (dm-restore-ws-from-overlay 
        dm (file (.getParent pdf) (str (chop-file-extension pdf) ".json"))))))


;;; LAYERS 

(defn- add-elements [layer item-list]
  (dorun (for [i item-list] (.addElement layer i)))
  item-list)

(defn- glean-visual-elements-1 [rep]
  (cond 
    (and (sequential? rep) (empty? rep)) '()
    (sequential? rep) (mapcat glean-visual-elements-1 rep)
    (not (map? rep)) '()
    (and (map? rep) (not (nil? (:items rep)))) (:items rep)
    :else
    (mapcat glean-visual-elements-1 (vals rep))))

(defn glean-visual-elements 
  "Return the Visual Elements in the given structured representation."
  [rep]
  (distinct (glean-visual-elements-1 rep)))

(defn- visual-el? [x] (instance? dray.j.VisualElement.El x))

(defn- key->string [x] 
  (cond (keyword? x) (.getName x) 
        (number? x) (str x)
        :else x))

#_(defn- replace-keys "Replace keys in map using f, a function over the key." [m f]
   (into {} (map (fn [[k v]] [(f k) v]) m)))

(defn- stringify-rep-maps [x]
  ;(println x)
  (cond 
    (and (coll? x) (not (map? x))) (map stringify-rep-maps x) ; recurse on lists.
    (or (visual-el? x) (not (map? x))) x ; unless true map, don't go further.
    (instance? dray.j.BoundingBox x) (bbox->bbox-map x)
    (map? x)  ; replace map keys.
      (apply hash-map (interleave (map key->string (keys x)) (map stringify-rep-maps (vals x))))
    :else  ; If here, didn't handle some non-map case - error.
    (uerr "Internal error - Can't handle case for %s" x)))


(defn make-layer
  "Create a new layer object with a given name, representation, and 
   element set. Omitted arguments default to empty lists. Keywords
   in the representation map are converted to strings."
  ([name representation elements]
    (let [layer (Layer. name)
          ensure-list #(if-not (coll? %) (list %) %)
          layer-rep (-> representation ensure-list stringify-rep-maps)]
      (println "Representation for " name " is " layer-rep)
      (doto layer
        (.setRep layer-rep)
        (add-elements elements))))
  ([name representation]
    (make-layer name representation (glean-visual-elements representation)))
  ([name] (make-layer name '() '())))

(defmethod print-method Layer [x writer]
  (.write writer (format "<Layer: %s (%d objs)>" (.getName x) (count (.getItems x)))))





