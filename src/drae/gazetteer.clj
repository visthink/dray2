(ns drae.gazetteer
  "Code for constructing a variety of classifiers (somewhat misnamed as Gazetteers at present)."
  (:import (com.leidos.bmech.analysis Gazetteer GeneGazetteer Evidence EvidenceTable))
  (:require [clojure.string :refer [lower-case]]
            [drae.corpus :refer [corpus]]
            [drae.doc :refer [get-vdocument]]
            [drae.manager :refer [make-data-manager dm-restore-ws-from-overlay]]
            [drae.wset :refer [ws-descendants]]
            [drae.vtable :refer [ws-vtables]]
            [drae.paxll :refer [model find-name-class]]) 
  )

;;; SIMPLE GAZETTEER - Constructed Gazetteer with map-based list.
;;; ----------------------------------------------------------------------------------

(defn get-item-class [gaz item]
  (let [table (.entry-table gaz)
        item-string (lower-case item)]
    (some (fn [k] (when (contains? (get @table k) item-string) k)) (keys @table))))

(defrecord simple-gazetteer [entry-table]
  Gazetteer
   (getItemClass [x item] nil)
   (addItemsToClass [x obj-list classname]  nil)
    )

(defmethod print-method Gazetteer [x writer]
  (.write writer (format "<Classifier/Gaz: %s (%d objs)>" (.getName x) (count (.getSet x)))))

(defn make-gazetteer 
  "Make an instance of a gazetteer, which can either be empty (holding nothing)
   or can have an initial class and set of members."
  ([] (->simple-gazetteer (atom '#{})))
  ([object-class object-list & more-class-list-pairs ]
    (let [string-list (map lower-case object-list)]
      (->simple-gazetteer (atom {object-class (set string-list)})))))


;;; BIOPAX Classifier - Classifier that classifies via lookup to BioPax model.
;;; ----------------------------------------------------------------------------------

(defn- translate-biopax-class  "Translate BioPax bioentity class to a Big Mechanism bioentity class."
  [biopax-class]
  (when-not (nil? biopax-class)
    (or (get '{:ProteinReference "protein_family", :Protein "gene_protein"} biopax-class)
        (.getName biopax-class))))

(defrecord biopax-classifier [model] ;; A simple gazetter-like classifier using a biopax model.
  Gazetteer
   (getItemClass [this item-name] (-> (find-name-class item-name (.model this))
                                    first translate-biopax-class))
   (getItemClasses [this item-name] (map translate-biopax-class 
                                         (find-name-class item-name (.model this))))
   (getName [_] "BIOPAX")
   )

(defmethod print-method biopax-classifier [x writer]
  (.write writer (format "<Classifier: %s (%d objs)>" "BioPax model" 
                         (count (.getObjects (.model x))))))

(defn make-biopax-classifier 
  "Create a simple text-based classifier based on this loaded BioPax model.
   Default is the current BioPax model."
  ([model]
    (->biopax-classifier model))
  ([] (make-biopax-classifier (model))))


;;; NUMBER CLASSIFIER - Classifier that classifies numbers based on regular expressions
;;;                      and the EDN read library.
;;; ----------------------------------------------------------------------------------

(defn- contains-numeral? [s] (some #(< 47 (int %) 58) s))

(defn clean-potential-number-string [s]
  (let [special #{\~ \# \$ \*}]
    (apply str (remove special (clojure.string/trim s)))))

;;; DOESN'T WORK WELL -- need a test suite for lots of different formats.

(defn classify-number-string [item-name]
  (when (contains-numeral? item-name)
    (let [res (clojure.edn/read-string item-name)]
      (if (number? res)
        (cond (float? res) '["Number" "Decimal"]
              (integer? res) '["Number" "Integer"]
              :else '["Number"])))))
  

(defrecord number-classifier []
  Gazetteer
  (getItemClass [this item-name] (first (classify-number-string item-name)))
  (getItemClasses [this item-name] (classify-number-string item-name))
  (getName [_] "NumClassifier")  
  )

(defn make-number-classifier []
  (number-classifier.))

(defmethod print-method number-classifier [x writer]
  (.write writer (format "<NumClassifier>")))

;;; HEADER CLASSIFIER - Uses regular expressions to match common headers in this domain.
;;; ----------------------------------------------------------------------------------

(def header-regexps 
  [:p-value
   :z-score
   :gene
   :peptide
   :baid
   :interactor
   :inhibitor
   :sequence
   :swiss-prot
   :kinase
   :residue
   :total
   :cancer]
  )


;;; Run Classifier on VTable
;;; ----------------------------------------------------------------------------------

(defn all-table-items [ws] 
  (set (mapcat #(.getItems %) (ws-descendants ws :tag "table"))))

(defn head-working-set [pdf overlay]  
  (.getHeadWorkingSet (-> pdf get-vdocument make-data-manager (dm-restore-ws-from-overlay overlay))))

(defn make-banner-gazetteer [pdf overlay]
  (let [headws (.getHeadWorkingSet (-> pdf get-vdocument make-data-manager (dm-restore-ws-from-overlay overlay)))
        exclusions (set (mapcat #(.getItems %) (ws-descendants headws :tag "table")))] ; Exclude table items.
    (GeneGazetteer. headws exclusions)))

(defn evidence-for-vcell [vcell, ^Gazetteer gaz]
  (let [txt (.getText vcell)
        classes (.getItemClasses gaz txt)]
    (when-not (nil? classes)
      (for [c classes]
        (Evidence. 0.70 (.getName gaz) c)))))

(defn evidence-for-vtable [vtable gaz]
  (for [vc (.getItems vtable) 
          :let [evlist (evidence-for-vcell vc gaz)]
          :when (not (nil? evlist))]
    [vc evlist]))
  
;;; EXAMPLES
;;; ----------------------------------------------------------------------------------


;;; NOT WORKING!

#_(defn update-evidence-table 
   "Check the vtable with the gazetteer, and update the ev-table accordingly."
   ([vtable gaz ev-table]
    ; (for [[vel evlist] (evidence-for-vtable vtable gaz)]
    (for [entry (evidence-for-vtable vtable gaz)]
      (let [[vel vlist] entry]
        (println "vel: " vel)
        (for [ev evlist]
          (.addEvidence ev-table vel ev)))
     ev-table))
   ([vtable gaz] (update-evidence-table vtable gaz (EvidenceTable.))))

  



