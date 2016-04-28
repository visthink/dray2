(ns dray.vtable
  "Factory functions and accessors for VTables, VCells, VRows, and VColumns.
   Also contains the `extract-vtables` routine, which takes labeled Table
   working sets and turns them into VTable structures,"
  (:import (com.leidos.bmech.analysis EvidenceTable)
           (com.leidos.bmech.model Layer LayerList WorkingSet DataManager)
           )
  (:require [clojure.java.io :refer [file]]
            [clojure.walk :refer [prewalk]]
            [dray.j.VisualElement :refer :all]
            [dray.corpus :refer [corpus]]
            [dray.doc :refer [bbox-union sort-vertically sort-horizontally
                              common-text-style get-vdocument]]
            [dray.wset :refer [ws-items ws-descendants]]
            [dray.manager :refer [make-data-manager dm-restore-ws-from-overlay]]
            )
  )
 
;;;; Building blocks for VTables
;;;; =====================================================================================

;;; VCELL 

(defn make-vcell "Create a new cell." 
  [items]
    (let [bbox (apply bbox-union (map :bbox items))
          style (common-text-style (map :style items))
          name (apply str (interpose " " (map #(.getText %) items)))]
      (->VCell name items bbox style))
    )

(defn make-vrow "Create a new row." 
  ([data-cells header-cells & name]
    (let [cells (concat header-cells data-cells)
          name (or name (str (gensym "row-"))) ; kludge
          bbox (apply bbox-union (map :bbox cells))
          style (common-text-style (map :style cells))
          ]
      (->VRow name (sort-horizontally data-cells) 
                   (sort-horizontally header-cells)
                   bbox style)))
  ([data-cells] (make-vrow data-cells '())))

(defn make-vcol "Create a new row." 
  ([data-cells header-cells & name]
    (let [cells (concat header-cells data-cells)
          name (or name 
                   (and (first header-cells) (.getText (first header-cells)))
                   (str (gensym "col-"))) ; Either take first header text or make gensym.
          bbox (apply bbox-union (map :bbox cells))
          style (common-text-style (map :style cells))]
      (->VCol name (sort-vertically data-cells) 
                   (sort-vertically header-cells)
                   bbox style)))
  ([data-cells] (make-vcol data-cells '())))

(defn make-vtable "Create a new table." 
  [name cells cols rows captions]
  (let [bbox (apply bbox-union (map :bbox cols))]
    (map->VTable {:name name :cells cells 
                  :cols (sort-horizontally cols)
                  :rows (sort-vertically rows)
                  :bbox bbox :evidence-table (EvidenceTable.)
                  :captions captions})))

(defmethod print-method dray.j.VisualElement.VTable 
  [vt writer]
  (.write writer 
    (format "<Table %s (%d cols, %d rows, %d cells)>" 
            (.name vt) (count (.cols vt)) (count (.rows vt))
            (count (.cells vt)))))

(defn vtable? "Is this an instance of VTable?" 
  [x] (instance? dray.j.VisualElement.VTable x))

(defn- layer-contains-vtable? "Does this layer contain a VTable rep?" 
  [l] (some vtable? (.getRep l)))

(defn layer-vtables "All VTables in the given Layer."
  [l] 
  (filter vtable? (.getRep l)))

(defn- ws-contains-vtable? "Does this WorkingSet contain a VTable?"
  [ws] 
  (some layer-contains-vtable? (vals (.getLayerList ws))))

(defn ws-vtables "Main function. Returns a list of all VTables in the given WorkingSet's layers."
  [ws] 
  (mapcat layer-vtables (filter layer-contains-vtable? (vals (.getLayerList ws)))))


;;;; Convert Working Sets with table and column tags into VTables and VColumns.
;;;; =====================================================================================

(defn- header-items "All visual elements in header working sets below this one."
  [ws]
  (map ws-items (ws-descendants ws :tag "header")))

(defn- ignore-set 
  "Set of items to ignore. E.g., all elements in ignore working sets
   below this one." 
  [ws]
  (set (mapcat ws-items (ws-descendants ws :tag "ignore"))))

(defn- row-sets 
  "Return a list of sets, each set representing all the items in a particular row."
  [ws]
  (map #(set (ws-items %)) (ws-descendants ws :tag "row")))

(defn- merge-sets
  "Return a list of sets, each representing the items in a single merge working-set."
  [ws]
  (map #(set (ws-items %)) (ws-descendants ws :tag "merge")))

(defn- containing-set
  "Given an item and a list of sets, return the first set that
   contains that item. Returns nil if no set found."
  [item list-of-sets]
  (some #(and (contains? % item) %) list-of-sets))

(defn- partition-by-sets
  "Given a list of elements and a list of sets, partitions the list
   so that sequential items in the same set are grouped. Items not
   in any set are partitioned based on their identity function (i.e.,
   usually placed into singleton groups."
  [item-list set-list]
  (partition-by #(or (containing-set % set-list) %) item-list))
  

;;; @@ We need to take the grouping sets, and then use that to partition
;;; the visual elements for the column. Then, we create cells based on
;;; those partitions and test it.
;;;

(defn- ws->vcol "Given a column-tagged working set, create a VColumn structure."
  [ws grouping-sets ignore-set]
  (let [header-items (header-items ws) ; List of lists.
        header-set (set (apply concat header-items))
        data-items (->> (ws-items ws) (remove ignore-set) (remove header-set)) 
        partitioned-data-items (partition-by-sets data-items grouping-sets)
        name (.getName ws)
        ] 
    (make-vcol (map make-vcell partitioned-data-items) 
               (map make-vcell header-items)
               name)))

(defn- vrows-from-vcols [vcols]
  ;; This is pretty limited -- we're just taking the cells in order.
  ;; To avoid error in apply/map, first make sure that vcols != nil.
  (if-not (empty? vcols)
    (let [row-lists (apply map list (map #(.data-items %) vcols))]
      (map make-vrow row-lists))))

;;; @@ Okay, we have the grouping sets to use for partitioning. Now we
;;; just need to modifiy ws->vcol appropriately (see above).
(defn- ws->vtable 
  "Given a table working set, attempt to create a VTable stucture using the 
   working set and its child working sets."
  [ws]
  (let [grouping-sets (concat (merge-sets ws) (row-sets ws))
        ignore-set (ignore-set ws)
        vcols (map #(ws->vcol % grouping-sets ignore-set) (ws-descendants ws :tag "column")) ; Make vcols.
        vrows (vrows-from-vcols vcols)
        captions (map #(make-vcell (ws-items %)) (ws-descendants ws :tag "caption")) ; Find caption(s).
        all-cells (mapcat ws-items vcols)] ; Gather cells.
    (make-vtable (.getName ws) all-cells vcols vrows captions)))


;;; Extract VTables multi-method
;;; --------------------------------------------------------------------------

(defmulti extract-vtables 
  "Extract a set of VTables from the given argument, which can be a DataManager or a WorkingSet
   that contains descendant working sets with the 'table' tag."
  class)

(defmethod extract-vtables DataManager [dm]
  (extract-vtables (.getHeadWorkingSet dm)))

(defmethod extract-vtables WorkingSet [ws]
  (into '() (map ws->vtable (ws-descendants ws :tag "table"))))

;;; Example construction
;;; --------------------------------------------------------------------------
;;; Two simple examples - EX1 and EX2

(defn- sibling-file 
  "Return sibling file given original and sibling filename." 
  [original sibname]
  (file (.getParent original) sibname))

(defn- ex1-files [] 
  (let [pdf (corpus :demo1 0)
        overlay (sibling-file pdf "00_Olds2009.json")]
    {:pdf pdf, :overlay overlay}))

(defn run-example 
  "Run a single example and return the resulting table."
  [{:keys [pdf overlay]}]
  (-> pdf get-vdocument make-data-manager (dm-restore-ws-from-overlay overlay) extract-vtables))

(defn ex1 
  "Run an example on the first demo pdf."
  [] 
  (run-example (ex1-files)))

;;; SIMPLIFY TABLE 
;;; --------------------------------------------------------------------------

(defn simplify-table 
  "Take the given table and strip out visual elements
   and bounding boxes."
  [table]
  (prewalk
    (fn [m] 
     #_ (println "At: " m)
      (if (map? m)
        (dissoc m :cells :bbox :style :evidence-table :items )
        m))
    table))

;;; DEMO EXAMPLES from DEMO1 Corpus
;;; --------------------------------------------------------------------------

(defn- overlay-for [pdf]
  (let [json-name-fn #(str (subs % 0 (- (count %) 3)) "json")]
    (file (.getParent pdf) (json-name-fn (.getName pdf)))
  ))

(defn- demo-files []
  (let [pdfs (corpus :demo1)]
    (for [pdf pdfs]
      {:pdf pdf, :overlay (overlay-for pdf)})))
  
(defn demo 
  "Run the nth demo pdf file and overlay from the Demo1 corpus."
  ([n] (run-example (nth (demo-files) n)))
  ([] (demo 0)))


