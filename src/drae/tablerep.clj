(ns drae.tablerep
  "Represent the characteristics of table entries."
  (:import 
    (java.util Collection HashMap)
    (drae.j.VisualElement VDocument)
    (com.leidos.bmech.model DataManager WorkingSet)
    (com.leidos.bmech.analysis Evidence)
    )
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :refer [file]]
    [drae.util :refer [uerr]]
    [drae.corpus :refer [corpus]]
    [drae.vtable :refer :all #_[make-vcol make-vcell make-vtable]] 
    [drae.paxll :refer :all])
  )

;;; Utility routines
;;; --------------------------------------------------------------

(def ^{:private true} find-name-class-memo (memoize find-name-class))

(def ^{:private true} find-bioentity-memo (memoize find-bioentity))

(defn- empty-key? 
  "True if the value for this map's key is an empty collection."
  [m k] 
  (let [v (get m k)
        can-be-empty? #(or (instance? Collection %) (instance? HashMap %) (coll? %))]
    (or (nil? v)
        (and (can-be-empty? v) (empty? v)))))

(defn- remove-empty-keys 
  "Remove entries that contain empty collections as a value."
  [m]
  (let [empty-keys (filter #(empty-key? m %) (keys m))]
    (apply dissoc m empty-keys)))

(defn hgnc-symbol 
  "Needs updating. Given an entity, attempts to find the HGNC cross-references
   by taking the set of cross-references, and looking for 'HGNC SYMBOL' as 
   the database source. Returns the symbol if found."
  [entity]
  ; (println "Looking for HGNC for " entity)
  (if-let [xrefs (.getXref entity)]
    (if-let [hgnc-xref (first (filter #(= "HGNC SYMBOL" (.getDb %)) xrefs))]
      (.getId hgnc-xref))))

(defn- singleton? [x] (= 1 (count x)))

(defn- ratios "Like frequencies function, but with ratios instead." [l] 
  (let [num-items (count l)]
    (into {} (map (fn [[k v]] [k (/ v num-items)]) (frequencies l)))))

(defn maybe-mult-nametexts? 
  "True if we have a set of names that are all relatively short, and the 
   list is longer than one."
  [name-list]
  (and (< 1 (count name-list) 20)
       (every? #(< (count %) 12) name-list)
       (every? #(not (.contains % " ")) name-list))) ;; No spaces in entries.
  
(defn vcell-nametexts
  "Determines if this cell is likely to actually be a set of nametexts, 
   rather than a single one, and attempts to parse it into multiple nametexts
   if so."
  [vcell]
  (let [subtexts (mapv #(.getText %) (.getItems vcell))]
    (if (maybe-mult-nametexts? subtexts)
      subtexts
      (list (.getText vcell)))))



;;; BioPax Representation (regular version)
;;; --------------------------------------------------------------

(defn simple-biopax-rep
  "Derive a simple BioPax representation by taking the bean of the object and 
   then removing the less relevant (or empty) slots)."
  [entity]
  (let [add-hgnc (fn [m e] ; Add HGNC symbol if found.
                   (if-let [sym (hgnc-symbol e)] 
                     (assoc m :hgnc sym)
                     m))]
    (-> (bean entity)
      remove-empty-keys
      (add-hgnc entity)
      (dissoc :comment :pk :displayName :class :modelInterface :standardName)
      (clojure.set/rename-keys {:name :aliases}))))

(defn rep-cell [vcell]
  (let [name (.getText vcell)
        classes (find-name-class-memo name)
        mdl (or (model) (uerr "No model defined - cannot retrieve representation for %s" vcell))]
    {:name (str vcell) 
     :depiction vcell
     :depiction-type :table-cell
     :depicts (for [cl classes]  
                (for [entity (common-generic-references (all cl :named name :in mdl))]
                  (into (sorted-map)
                        (merge
                          {:name (format "<%s (%s)>" name entity)
                           :class (.getName cl)
                           :entity entity
                           }
                          (simple-biopax-rep entity)
                        ))))
     :items (.getItems vcell)
     :class (if (singleton? classes) (first classes) :unresolved)
     }
    ))

(defn rep-column [vcol]
  (let [cell-reps (map rep-cell (.data-items vcol))
        potential-classes (ratios (map :class cell-reps))]
    {:name (str vcol)
     :depiction vcol
     :depiction-type :table-column
     :depicts []
     :parts cell-reps
     :potential-classes potential-classes
     :class (if (singleton? potential-classes) (first potential-classes) :unresolved)
     }
  ))

(defn rep-table [vtable]
  (println "Starting to represent " (str vtable) "...")
  (let [col-reps (map rep-column (.cols vtable))
        table-rep
        {:name (str vtable)
         :depiction vtable
         :depiction-type :table
         :depicts []
         :parts col-reps
         }]
    (println "   Represented table and " (count col-reps) "columns.")
    table-rep))
    

;;; BioPax Representation (simpler version)
;;; --------------------------------------------------------------

(defn simpler-biopax-rep [entity]
  (let [sort-map #(into (sorted-map) %)
        add-hgnc (fn [m e] ; Add HGNC symbol if found.
                  (if-let [sym (hgnc-symbol e)] 
                    (assoc m :hgnc sym)
                    m))
        update-organism (fn [m] (if-let [o (:organism m)]
                                  (assoc m :organism (first (.getName o)))))]
    (-> (simple-biopax-rep entity)
      update-organism
      (select-keys [:name :class :organism :RDFId])
      (add-hgnc entity)
      sort-map)))

(defn srep-nametext [nametext]
  (let [entity-classes (find-name-class-memo nametext)
        mdl (or (model) (uerr "No model!"))
        ]
    (apply concat
           (for [cl entity-classes]
             (for [entity (common-generic-references (all cl :named nametext :in mdl))
                   :when (not (nil? (hgnc-symbol entity)))]
               (merge {:name nametext
                       :class (.getName cl)
                       }
                      (simpler-biopax-rep entity)))))))

(defn srep-cell [vcell]
  (let [names (vcell-nametexts vcell)
        namereps (mapcat srep-nametext names)
        entity-classes (distinct (map :class namereps))
        entity-class (if (singleton? entity-classes) (first entity-classes) :unresolved)
        sort-map #(into (sorted-map) %)]
    (-> {:name (.getText vcell)
         :class "tablecell"
         :entity-classes entity-classes
         :entity-class entity-class
         :entities namereps
         }
      remove-empty-keys
      sort-map)))

  
(defn srep-col [vcol] 
  (let [cell-reps (map srep-cell (.data-items vcol))
        potential-classes (ratios (map :entity-class cell-reps))]
    {:name (str vcol)
     :class "col"
     :parts cell-reps
     :potential-classes potential-classes
     }
    ))

(defn srep-table [vtable]
  (println "Creating simple representation for " (str vtable) "...")
  (let [col-reps (mapv srep-col (.cols vtable))
        table-rep
         {:name (.getName vtable)
          :class "table"
          :cols col-reps
          }]
    table-rep))

;;; Handling the test set.
;;; -----------------------------------------------------------------------------

(defn simple-table-rep [vtable]
  (let [res  (srep-table vtable)]
    (with-out-str (json/pprint res))))

(defn json-fname-for [vtable pdf]
  (let [root #(subs % 0 (- (count %) 4))]
    (file (.getParent pdf)
          (str (root (.getName pdf)) "_" (.getName vtable) ".json"))))

(defn simple-table-rep-table [vtable pdf]
  (let [json-fname (json-fname-for vtable pdf)
        rep (srep-table vtable)]
    (spit json-fname (with-out-str (json/pprint rep)))
    rep))


(defn run-demotest1 []
  (for [n (range 5)]
    (let [pdf (corpus :demo1 n)]
      (println "Looking at PDF: " pdf)
      (let [vtables (demo n)]
        (for [vt vtables]
          (do 
            (println "   table: " (.getName vt))
            (simple-table-rep-table vt pdf)))))))

;;;; Useful print-methods


(defmethod print-method org.biopax.paxtools.impl.level3.RelationshipXrefImpl [x writer]
  (.write writer (format "<Xref %s>" (str x))))

(defmethod print-method org.biopax.paxtools.impl.level3.ModificationFeatureImpl [x writer]
  (.write writer (format "<%s>" (str x))))

(defmethod print-method org.biopax.paxtools.impl.level3.FragmentFeatureImpl [x writer]
  (.write writer (format "<FragmentFeature: %s>" (str x))))
