(ns drae.j.VisualElement
  "Definitions for various vel (visual element) types. These types follow the
   PDF element types fairly closely, and so contain more style information than 
   the original vels."
  (:import (java.awt.Point)
           (java.lang Double)
           (java.awt.geom Rectangle2D Line2D)
           (java.util ArrayList)
           )
  (:require [clojure.string :as s]
            [clojure.java.io :refer [file]]
            [drae.util :refer [pairwise-split-when uerr]]
            )
  )

(declare split-items-at-pos split-items-at-separator)

;;; Bounding Boxes
;;; ------------------------------------------------------------------------------
(gen-class :name drae.j.BoundingBox
           :extends java.awt.geom.Rectangle2D$Double
           :prefix "bbox-")

(defn bbox-toString [this]
  (format "<bbox @(%.0f,%.0f) [%.1f x %.1f]>" 
          (.getX this) (.getY this) (.getWidth this) (.getHeight this)))

(defmethod print-method drae.j.BoundingBox [x writer]
  (.write writer (bbox-toString x)))

(defn my-bbox-union 
  "Create a new bounding box that contains all the given bounding boxes. May be a little slow." 
  [& bboxes]
  (let [u (reduce (fn [^Rectangle2D b1, ^Rectangle2D b2] (.createUnion b1 b2)) bboxes)]
    (drae.j.BoundingBox. (.getX u) (.getY u) (.getWidth u) (.getHeight u))))

;;; Text styles
;;; ------------------------------------------------------------------------------

(defrecord TextStyle [font-name, font-size,
                      ^Boolean bold, ^Boolean italic, color]
  )


;;; Top-level Java interface for bounding box and subitems.
;;; ------------------------------------------------------------------------------

(defprotocol El
  #_"Any type of visual element with a boundary."
  (getItems [this] "Subitems, if any.")
  (setItems [this items] "Set items (not supported for all visual elements.")
  (getBbox [this] "Boundary, if sensible.")
  (getText [this] "Contained text, if sensible.")
  (withRevisedItems [x items] "Return copy of element with revised item set and bounding box.")
  (splitEl [this] [this y-pos]
                 "Split into set of constituent items. With no arguments, split into all subelements. 
                  With y-pos, split at the y-pos if possible.")
  (splitAtSeparator [this separator]
                       "Split all vertical texts along this separator, if the separator crosses them 
 along whitespace.")
  (replaceElWith [x old-item new-items] 
                   "Replace a single item with one or more new items in place, returning
                    new visual element."))

(defprotocol HasFilename
  (getFilename [x] "Filename for the object, if any."))

(defprotocol Table 
  (getCells [x] "List of cells in the table.")
  (getName [x] "Name of the table.")
  (getRows [x] "Rows, in matrix style.")
  (getCols [x] "Columns, in matrix style.")
  (getRowsAsSesp [x] "Rows, as an s-expression.")
  (getColsAsSesp [x] "Columns, as an s-expression.")
  (getDataRows [x] "Data rows only.")
  (getDataCols [x] "Data columns only.")
  (getCaptions [x] "Captions only.")
  )

(defprotocol HasHeaders
  (getDataItems [x] "Data items only.")
  (getHeaderItems [x] "Header items only.")
  )

(defn items [e] (.getItems e))


;;; Utility routines
;;; ------------------------------------------------------------------------------
(defn replace-item-with 
  "Replace this first instance of *item* in the list *mylist* with *replacement-items*."
  [mylist item replacement-items]
  (let [[start-items end-items] (split-with #(not (= % item)) mylist)]
    (if (empty? end-items)
      mylist ;; Not found, so no change.
      (into '[] (concat start-items replacement-items (rest end-items))))))



;;; Documents
;;; ------------------------------------------------------------------------------

(deftype VDocument [^String filename, ^:volatile-mutable ^java.util.List items]
 Object
 (^String toString [_] (format "<VDocument2 %s (%d pp)>" filename (count items)))
 El
 (getItems [_] ^java.util.List items)
 (setItems [_ new-items] (set! items new-items))
 (withRevisedItems [this new-items] (doto this (.setItems this new-items)))
 (replaceElWith [this-doc old-vtext new-vtexts]
   (into '[] (for [page (.getItems this-doc)]
               (if (some #(= % old-vtext) (.getItems page))
                 (replaceElWith page old-vtext new-vtexts)
                 page))))
 HasFilename
  (getFilename [_] filename)
 )
  
(defmethod print-method drae.j.VisualElement.VDocument [x writer]
  (.write writer (.toString x)))

;;; Images
;;; ------------------------------------------------------------------------------

(defrecord VImage [bitmap-path bbox]
  Object
  (toString [this] (format "<VImage %s>" (.bitmap-path this)))
  El
  (getBbox [this] (.bbox this))
  (getItems [this] [])
  )

(defmethod print-method VImage [x writer]
  (.write writer (.toString x)))

;;; Diagrams
;;; ------------------------------------------------------------------------------
(defrecord VDiagram [image bbox items]
  Object
  (toString [this] (format "<VDiagram %s (%d items)>" (.image this) (count (.items this))))
  El
  (getBbox [this] (.bbox this))
  (getItems [this] (.items this))
  (withRevisedItems [this new-items] (->VDiagram (.image this) (.bbox this) new-items))
  )

(defmethod print-method VDiagram [x writer]
  (.write writer (.toString x)))

;;; Pages
;;; ------------------------------------------------------------------------------

;;; This is a new version of the VPage class, but as a type rather than a record.
;;; This version has a mutable items field, which is used for merging and splitting
;;; individual texts during document analysis.

(deftype VPage [^Integer number, ^drae.j.BoundingBox bbox, ^:volatile-mutable ^java.util.List items]
  Object
  (toString [_] (str "<VPage2 #" number ">"))
  El
  (getItems [_] items)
  (setItems [_ new-items] (set! items new-items))
  (getBbox [_] bbox)
  (getText [_] (apply str (interpose " " (map #(.getText %) items))))
  (withRevisedItems [this new-items] (doto this (.setItems new-items)))
  (replaceElWith [this old-vtext new-vtexts]
    (withRevisedItems this (replace-item-with (.getItems this) old-vtext new-vtexts)))
  (splitEl [this xpos] 
    (withRevisedItems this (split-items-at-pos (.getItems this) xpos)))
  (splitAtSeparator [this separator]
    (withRevisedItems this (split-items-at-separator (.getItems this) separator)))
    
  )

(defmethod print-method VPage [x writer]
  (.write writer (.toString x)))

;;; Text lines
;;; ------------------------------------------------------------------------------

;;; The next few functions are copied from the drae.doc code. We don't include it
;;; directly due to the resultkng circular package reference.

(defn my-make-text-style 
  "Create a text style object."
  ([fname size bold italic color]
    (let [size-or-nil (if-not (nil? size) (Double. size))]
    (->TextStyle fname size-or-nil bold italic color)))
  ([fname size]
    (my-make-text-style fname size nil nil nil)))

(defn- common-aspect 
  "Check the sequence of items to see if it has a common aspect. Return 
   the aspect if there is one, and nil if there is not. If second argument
   is omitted, defaults to identity."
  ([s] 
    (let [[f & r] s]
      (cond (empty? r) f
            (every? #(= % f) r) f
            :else nil)))
  ([s fn] (common-aspect (map fn s))))

(defn- my-common-text-style 
  "Given a sequence of text styles, return a text style with the values
   set for all common aspects, but with NIL for the non-common aspects."
  [text-style-list]
  (my-make-text-style 
    (common-aspect text-style-list #(and (.font-name %)
                                         (s/lower-case (.font-name %))))
    (common-aspect text-style-list :font-size)
    (common-aspect text-style-list :bold)
    (common-aspect text-style-list :italic)
    (common-aspect text-style-list :color)))

(declare ->VText ) ; Forward reference

(defn- my-make-vtext "Make a single instance of a VText." 
  [tokens]
  (let [bbox (apply my-bbox-union (map #(.getBbox %) tokens))
        text-line (apply str (interpose " " (map #(.getText %) tokens)))
        style (my-common-text-style (map #(.style %) tokens))]
    (->VText text-line bbox tokens style)))


(defrecord VText [^String text, ^drae.j.BoundingBox bbox, ^java.util.List items, ^TextStyle style]
  Object
  (toString [this] 
    (let [x (.x (.bbox this))
          y (.y (.bbox this))]
      (format "<VText (%.1f,%.1f) \"%s\">" x y (.text this))))
  El
  (getItems [x] (.items x))
  (getBbox [x] (.bbox x))
  (getText [x] (.text x))
  (splitEl 
    [this] (into '[] (for [vtok (.items this)] 
                       (->VText (.text vtok) (.bbox vtok) (list vtok) (.style vtok)))))
  (splitEl
    [this xpos] 
    (let [between? (fn [a b] (< (.. a getBbox getMaxX) xpos (.. b getBbox getX)))
          [head tail] (pairwise-split-when between? (.getItems this))]
      (if-not (empty? tail)
        (do 
          (println (format "Splitting %s into %s and %s." this head tail))
          [(my-make-vtext head) (my-make-vtext tail)])
        [this]))); If failed, return original text in list.
  )
  

(defn split-items-at-pos 
  "Given a list of items, find all items that cross the given xpos, and split
   them if they cross it at a whitespace (i.e., between tokens)."
  [item-list xpos]
  (let [splittable? #(and (instance? VText %)
                          (< (.. % getBbox getX) xpos (.. % getBbox getMaxX)))]
    (into '[]
          (reduce (fn [sofar item] 
                    (if (splittable? ^VText item)
                      (concat sofar (.splitEl item xpos)) ;; Attempt to split.
                      (conj sofar item))) ;; Don't split
                  '[]
                  item-list))))

(defn split-items-at-separator
  "Given a list of items and a separator line segment (which must be 
   vertical), break apart all vtexts that have whitespace along that line."
  [^java.util.List item-list, ^java.awt.geom.Line2D separator]
  (let [y1 (.getY1 separator), y2 (.getY2 separator)
        minY (min y1 y2), maxY (max y1 y2),
        within-y-range? (fn [bbox] (< minY (.getMinY bbox) (.getMaxY bbox) maxY))
        xpos (.getX1 separator)
        vertical? (= xpos (.getX2 separator))
        splittable? #(< (.. % getBbox getX) xpos (.. % getBbox getMaxX))
        ]
    (when-not vertical? ;; 
      (uerr "Separator must be vertical - %s" separator))
    (into '[]
      (reduce (fn [sofar item]
                (if (and (instance? VText item)
                         (within-y-range? (.getBbox item))
                         (splittable? item))
                  (concat sofar (.splitEl item xpos)) ;; Attempt to split.
                  (conj sofar item))) ; Don't attempt, just add existing.
              '[]
              item-list))))



;;
;; Here we're going to try to write the split that takes a single y parameter.
;; 


(defmethod print-method VText [x writer]
  (.write writer (.toString x)))

;;; Text tokens
;;; ------------------------------------------------------------------------------

(defrecord VTextToken [^String text, ^drae.j.BoundingBox bbox, style]
  Object
   (toString [this]
     (format "<token \"%s\">" (.text this)))
  El
   (getItems [x] []#_(java.util.ArrayList.))
   (getBbox [x] (.bbox x))
   (getText [x] (.text x)))

(defmethod print-method VTextToken [x writer]
  (.write writer (.toString x)))

;;; Text Blocks
;;; ------------------------------------------------------------------------------
(defrecord VTextBlock [items bbox style]
  Object
  (toString  [this]
    (format "<textblock (%d items)>" (count items)))
  El
  (getItems [x] (.items x))
  (getBbox [x] (.bbox x))
  (getText [x] (apply str (interpose " " (map #(.getText %) (.items x)))))
  )

(defmethod print-method VTextBlock [x writer]
  (.write writer (.toString x)))


;;; Table Cells
;;; ------------------------------------------------------------------------------
;;; For now, table cells look a lot like text blocks.
(defrecord VCell [name items bbox style]
  Object
  (toString [this]
    (format "<Cell %s>" (.name this)))
  El
  (getItems [x] (.items x))
  (getBbox [x] (.bbox x))
  (getText [x] (apply str (interpose " " (map #(.getText %) (.items x)))))
 )

(defmethod print-method VCell [x writer]
  (.write writer (.toString x)))


;;; Table Rows
;;; ------------------------------------------------------------------------------

(defrecord VRow [name data-items header-items bbox style]
   Object
  (toString [this]
    (format "<Row %s>" (.name this)))
  El
  (getItems [x] (concat (.header-items x) (.data-items x)))
  (getBbox [x] (.bbox x))
  HasHeaders
  (getDataItems [x] (.data-items x))
  (getHeaderItems [x] (.header-items x))
  )

(defmethod print-method VRow [x writer]
  (.write writer (.toString x)))


;;; Table Columns
;;; ------------------------------------------------------------------------------

(defrecord VCol [name data-items header-items bbox style]
  Object
  (toString [this]
    (format "<Col %s>" (.name this)))
  El
  (getItems [x] (concat (.header-items x) (.data-items x)))
  (getBbox [x] (.bbox x))
  HasHeaders
  (getDataItems [x] (.data-items x))
  (getHeaderItems [x] (.header-items x))
  )

(defmethod print-method VCol [x writer]
  (.write writer (.toString x)))

;;; Table 
;;; ------------------------------------------------------------------------------

(defrecord VTable [name cells bbox rows cols #_header-rows #_header-cols captions evidence-table]
  Object
  (toString [this] (format "<Table %s>" (.name this)))
  El
  (getItems [x] (.cells x))
  (getBbox [x] (.bbox x))
  Table
  (getName [x] (.name x))
  (getCells [x] (.cells x))
  (getRows [x] (.rows x))
  (getCols [x] (.cols x))
  #_(getHeaderRows [x] (.header-rows x))
  #_(getHeaderCols [x] (.header-cols x))
  )

  