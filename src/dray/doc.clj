(ns dray.doc
  "*Main routines for documents, pages, and visual elements.
    Contains visual element constructors and accessors, along
    with several routines for manipulating sets of visual 
    elements.*"
  (:import (java.awt.geom Rectangle2D)
           (dray.j BoundingBox)
           (dray.j.VisualElement VDocument VText VPage)
           )
  (:require [clojure.string :as s]
            [clojure.data.xml :refer [parse-str]]
            [clojure.zip :refer [xml-zip up down right node end? edit root]]
            [dray.util :refer [resolve-cache-file-for cache-directory pairwise-replace 
                               decode-url pairwise-partition-when uerr]]
            [clojure.java.io :refer [file]]
            [dray.util.math :refer [abs-diff]]
            [dray.xml :refer [cljxml->prxml prxml-tag prxml-attrs prxml-content]]
            [dray.ext.pdftoxml :refer [pdftoxml-output]]
            [dray.j.VisualElement :refer :all]
            )
  )
 
;;; BOUNDING BOXES

(defn make-bbox 
  "Creates instance of a bounding box object (a subclass of java.awt.geom.Rectangle2D.Double).
   Arguments can be numbers or strings that are parsable to numbers."
  ([x y width height]
    (let [->dbl #(if (string? %) (Double/parseDouble %) (double %))] ; Needed to handle doubles, ints *and* strings.
      (dray.j.BoundingBox. (->dbl x) (->dbl y) (->dbl width) (->dbl height))))
  ([arg-list] (let [[x y width height] arg-list] (make-bbox x y width height))))

(defn bbox-union 
  "Create a new bounding box that contains all the given bounding boxes. May be a little slow." 
  [& bboxes]
  (if-not (empty? bboxes)
   (let [u (reduce (fn [^Rectangle2D b1, ^Rectangle2D b2] (.createUnion b1 b2)) bboxes)]
     (make-bbox (.getX u) (.getY u) (.getWidth u) (.getHeight u)))))


(defn bbox->bbox-map
  "Write a simple representation for a bounding box."
  [bbox]
  {:x (.getX bbox)
   :y (.getY bbox)
   :width (.getWidth bbox)
   :height (.getHeight bbox)
   }
  )

(defn bbox? [x] (instance? dray.j.BoundingBox x))


  
;;; Selector and helper functions
;;; ------------------------------------------------------------------------------

(defn vel-x "Element X coord (LHS)." [vel] (.getX (.getBbox vel)))

(defn vel-y "Element Y coord (top)." [vel] (.getY (.getBbox vel)))

(defn vel-width "Element width." [vel] (.getWidth (.getBbox vel)))

(defn vel-height "Element height." [vel] (.getHeight (.getBbox vel)))

(defn vel-max-x "Element rightmost x coord (RHS)." [vel] (.getMaxX (.getBbox vel)))

(defn vel-max-y "Element bottom y coord." [vel] (.getMaxY (.getBbox vel)))

(defn vel-center-x "Element center x coord." [vel] (.getCenterX (.getBbox vel)))

(defn vel-center-y "Element center y coord." [vel] (.getCenterY (.getBbox vel)))

;;; Relations between visual elements.

(defn valigned? "Are these two elements vertically aligned?" 
  [vel1 vel2]
  (> (vel-height vel1) (abs-diff vel-y vel1 vel2)))

(defn text-distance 
  "Distance between right edge of first element and left edge of second."
  [vel1 vel2]
  (- (vel-x vel2) (+ (vel-x vel1) (vel-width vel1))))

(defn texts-adjacent?
  "Heuristic test to see if two texts are adjacent. Rule is distance must
   be less than average character height."
  [txt1 txt2]
  (and (instance? dray.j.VisualElement.VText txt1)
       (instance? dray.j.VisualElement.VText txt2)
       (valigned? txt1 txt2)
       (< -0.1 (text-distance txt1 txt2) (vel-height txt1)))) ; Allows some overlap.


;;; Sorting
;;; ------------------------------------------------------------------------------


(defn sort-horizontally
  "Given a list of visual elements with bounding boxes, sort
   the list spatially from left-to-right."
  [vel-list]
  (sort-by vel-x vel-list))

(defn sort-vertically
  "Given a list of visual elements with bounding boxes, sort
   the list spatially from top to bottom."
  [vel-list]
  (sort-by vel-y vel-list))

  
;;; TEXT STYLES

(defn- string->boolean 
  "Interpret yes/no/true/false string as boolean."
  [s]
  (cond (nil? s) nil
        (= true s) true
        (= false s) false
        (not (instance? String s)) nil
        :else   
        (let [s1 (s/lower-case s)]
          (cond 
            (contains? #{"true" "t" "yes"} s1) true
            (contains? #{"false" "f" "no"} s1) false
            :else nil))))

(defn make-text-style 
  "Create a text style object."
  ([fname size bold italic color]
    (let [size-or-nil (if-not (nil? size) (Double. size))]
    (->TextStyle fname size-or-nil 
                 (string->boolean bold)
                 (string->boolean italic)
                 color)))
  ([fname size]
    (make-text-style fname size nil nil nil)))

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

(defn common-text-style 
  "Given a sequence of text styles, return a text style with the values
   set for all common aspects, but with NIL for the non-common aspects."
  [text-style-list]
  (make-text-style 
    (common-aspect text-style-list #(and (.font-name %)
                                         (s/lower-case (.font-name %))))
    (common-aspect text-style-list :font-size)
    (common-aspect text-style-list :bold)
    (common-aspect text-style-list :italic)
    (common-aspect text-style-list :color)))
  
;;; DOCUMENTS

(defn make-vdocument 
  "Create a single instance of a VDocument."
  [filename items]
  (->VDocument filename items))
             
(defn pages "Collection of pages in a VDocument." [vdoc]
  (.getItems vdoc))


;;; VIMAGES

(defn make-vimage 
  "Create a single instance of a VImage"
  [bitmap-path bbox]
  (->VImage bitmap-path bbox))

(defn full-bitmap-path-for 
  "Given a VImage instance and its containing VDocument, return the 
   full bitmap path for the image file representing the VImage instance."
  [vimage vdocument]
  (let [cache-dir (cache-directory (file (.filename vdocument)))]
    (file cache-dir (.bitmap-path vimage))))

;;; VDIAGRAMS

(defn make-vdiagram 
  "Create an instance of VDiagram from the given VImage instance and a set
   of items contained within the diagram."
  [image items]
  (->VDiagram image (.bbox image) items))

(defn make-vpage [page-no bbox items]
  (->VPage (Long. page-no) bbox items))


;;; VTEXTS

(defn make-vtext "Make a single instance of a VText." 
  [bbox tokens]
  (let [text-line (apply str (interpose " " (map :text tokens)))
        item-list #_(ArrayList. tokens) tokens
        style (common-text-style (map :style tokens))]
    (->VText text-line bbox item-list style)))


(defn merge-vtexts 
  "Merge the two vtexts, which are assumed to be in order. Merges all the tokens,
   and creates a style that is held in common."
  [vtext1 vtext2]
  (let [tokens (concat (.getItems vtext1) (.getItems vtext2))
        bbox (bbox-union (.getBbox vtext1) (.getBbox vtext2))]
    (make-vtext bbox tokens)))

;;; MERGING ADJACENT VTEXTS

(defmulti merge-adjacent-vtexts 
  "Given a set of vtexts (or a VPage or VDocument), look through 
   all the visual elements and merge any adjcent pairs of vtexts."
  class)

(defn- merge-adjacent-vtexts-in-list [l]
  (pairwise-replace l texts-adjacent? merge-vtexts))

(defn- merge-adjacent-vtexts-in-page [page]
  (make-vpage (.number page) (.bbox page) (merge-adjacent-vtexts-in-list (.getItems page))))

(defn- merge-adjacent-vtexts-in-vdoc [vdoc]
  (make-vdocument (.filename vdoc) (map merge-adjacent-vtexts (pages vdoc))))

(defmethod merge-adjacent-vtexts java.util.List [vel-list]
  (merge-adjacent-vtexts-in-list vel-list))
;  (pairwise-replace vel-list texts-adjacent? merge-vtexts))
  
(defmethod merge-adjacent-vtexts dray.j.VisualElement.VPage [page]
  (merge-adjacent-vtexts-in-page page))
;  (make-vpage (.number page) (.bbox page) (merge-adjacent-vtexts (.getItems page))))

(defmethod merge-adjacent-vtexts dray.j.VisualElement.VDocument [vdoc]
  (merge-adjacent-vtexts-in-vdoc vdoc))
;  (make-vdocument (.filename vdoc) (map merge-adjacent-vtexts (pages vdoc))))

;; Find VTexts that contain odd spacing that may indicate two badly-merged
;; VTexts.

(defn partition-vtext-by-whitespace 
  "Returns nil if no paritioning is needed."
  ([vtext margin]
    (let [tokens (.getItems vtext)]
      (cond
        (not (< 1 (count tokens) 4)) nil 
        :else 
         (pairwise-partition-when #(> (text-distance %1 %2) margin) tokens))))
  ([vtext]
    (partition-vtext-by-whitespace vtext (* 1.1 (vel-height vtext)))))

(defn- token-set-to-vtext [token-list]
  (make-vtext (apply bbox-union (map #(.bbox %) token-list)) token-list))

(defn- split-distant-vtexts-in-list [vtexts]
  (loop [head '[], tail vtexts]
    (if (empty? tail) head 
      (let [vtext (first tail)
            res (partition-vtext-by-whitespace vtext)]
        (if (or (nil? res) (nil? (second res))) ; if not splittable,
          (recur (conj head vtext) (rest tail)) ;  just add vtext to list.
          (let [replacement-vtexts (mapv token-set-to-vtext res)]
            (println (format "Replacing %s with %s." vtext replacement-vtexts))
            (recur (concat head replacement-vtexts) (rest tail)))))))) ; otherwise add split items.

(defn- split-distant-vtexts-in-page [page]
  (make-vpage (.number page) (.bbox page) (split-distant-vtexts-in-list (.getItems page))))

(defn- split-distant-vtexts-in-vdoc [vdoc]
  (make-vdocument (.filename vdoc) (map split-distant-vtexts-in-page (pages vdoc))))

(def split-distant-vtexts 
  "Review all the items in the given document and split those vtexts (i.e., text lines)
   that are likely to be close column cells. Uses the text height as a heuristic to 
   find vtokens that are distant."
  split-distant-vtexts-in-vdoc)

;;; VTEXT-TOKEN

(defn make-vtext-token "Make a single vtext token." [text bbox style]
  (->VTextToken text bbox style))


;;; VTEXT-BLOCK

(defn make-vtext-block "Create a new text block." [items]
  (let [bbox (apply bbox-union (map :bbox items))
        style (common-text-style (map :style items))]
    (->VTextBlock items bbox style)))

;;; VDOCUMENTS

;;; Visual elements from P2X file (via PRXML format)
;;; ----------------------
(defn- next-up 
  "Next location at or above the current node level (i.e., not down)."
  [loc]
  (when-not (or (nil? loc) (end? loc))
    (or (right loc)
        (loop [l loc c 10]
          (if (or (nil? l) (zero? c)) nil
            (or (right (up l))
                (recur (up l) (- c 1))))))))

(defn prxml-vector-xml
  "Given a filename, return the XML from that vector file."
  [href pdf]
  (let [vecfile (resolve-cache-file-for pdf (file (decode-url href)))]
    (-> vecfile slurp parse-str)))

(defn- edit-include-node [xml-node pdf]
  (prxml-vector-xml (get-in xml-node [:attrs :href]) pdf))

(defn- add-included-xml 
  [x pdf]
  (loop [loc (xml-zip x)]
    (case (:tag (node loc))
      :DOCUMENT (recur (down loc))
      :PAGE (recur (down loc))
      :include (let [edited-loc (edit loc edit-include-node pdf)]
                 (if-let [nxt (next-up edited-loc)] (recur nxt) (root edited-loc)))
      (if-let [nxt (next-up loc)] (recur nxt) (root loc)))))  
  
(defn p2x-prxml 
  "Returns the PRXML information for the given PDF by running
   it through pdftoxml, and reading in the result."
  [pdf]
  (-> (pdftoxml-output pdf)
    slurp
    parse-str
    (add-included-xml pdf) ;; Add included vector elements. 
    cljxml->prxml))

(defmulti prxml->vel 
  "Translate a single prxml node into a visual element object or nil (if not applicable)."
  prxml-tag
  )

(defmethod prxml->vel :default [node]  nil) ;; Default is to return nil.

(defmethod prxml->vel :DOCUMENT [node]
  (let [fname (-> node prxml-content first prxml-content first prxml-content first) ; kludge
        items (filter (comp not nil?) (map prxml->vel (prxml-content node)))]
    (make-vdocument fname items)))

(defmethod prxml->vel :PAGE [node]
  (let [{:keys [width height number]} (prxml-attrs node)
        bbox (make-bbox 0.0 0.0 width height)
        items (filter (comp not nil?) (map prxml->vel (prxml-content node)))]
    (make-vpage number bbox items)))

(defmethod prxml->vel :TEXT [node]
  (let [{:keys [x y width height]} (prxml-attrs node)
        bbox (make-bbox x y width height)
        tokens (map prxml->vel (prxml-content node))]
    (make-vtext bbox tokens)))

(defmethod prxml->vel :TOKEN [node]
  (let [attrs (prxml-attrs node)
        {:keys [x y width height]} attrs
        bbox (make-bbox x y width height)
        {:keys [font-name font-size bold italic font-color]} attrs
        style (make-text-style font-name font-size bold italic font-color)
        ]
    (->VTextToken (first (prxml-content node)) bbox style)))
     
(defmethod prxml->vel :IMAGE [node]
  (let [attrs (prxml-attrs node)
        {:keys [x y width height]} attrs
        bbox (make-bbox x y width height)
        bitmap-path (:href attrs)] 
    (make-vimage bitmap-path bbox)))

(defn ^dray.j.VisualElement.VDocument get-vdocument
  "Return the VDocument record object for the given PDF file. Currently
   handles only text and text tokens."
  [pdf]
  (println "Getting vdocment: " pdf)
  (when-not (.exists pdf) (uerr "Could not locate file %s" pdf))
  (-> pdf p2x-prxml prxml->vel merge-adjacent-vtexts-in-vdoc split-distant-vtexts))

