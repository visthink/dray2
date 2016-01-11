(ns 
  ^{:doc 
    "**paxll - PAXtools Lilliputian Language**

     Paxll is a tiny little language for first-step reasoning over a Paxtools ontology. Not as 
     pretty or graphically-oriented as ChiBE, not as powerful as Paxtools
     (on which it is built), and with a syntax that experts may find 
     frustratingly uncluttered and concise, *paxll* nevertheless should be 
     quite useful for a number of tasks.  

     Paxll also includes a simple method for Pathway Commons 2 web search (based on cpath).

     Examples:

     `(models)`                ; List the set of known models.
     
     `(model! :raf-cascade)`   ; Load the RAF Cascade model, and set it as the default.

     `(all :Protein )`         ; All the proteins in the RAF Cascade model.

     `(all :Protein :named \"cRaf\")` ; Attempt to find the cRaf protein itself.

     `(aliases (all :Protein :named \"cRaf\"))` ; All aliases for cRaf.

     "
    
    :author "Ron Ferguson"}

  drae.paxll

  (:import (java.net InetAddress)
           (java.util HashSet)
           (java.lang Class)
           (org.biopax.paxtools.model BioPAXLevel Model)
           (org.biopax.paxtools.model.level3 Protein ProteinReference Pathway Conversion Named SimplePhysicalEntity)
           (org.biopax.paxtools.io SimpleIOHandler)
           (org.biopax.paxtools.pattern Pattern Constraint Searcher PatternBox MappedConst)
           (org.biopax.paxtools.pattern.util RelType)
           (org.biopax.paxtools.pattern.constraint ConBox Participant ParticipatesInConv 
                                                   Type PathConstraint ConstraintAdapter)
           (drae.j.paxtools XPattern XMatch)
           )
  (:require [clojure.repl :refer :all]
            [clj-http.lite.client :as client]
            [clojure.data.xml :as xml]
            [clojure.data.json :as json]
            [clojure.string :refer [lower-case upper-case capitalize]]
            [clojure.java.io :refer [input-stream file]]
            [drae.util :refer [uerr file?]]
            [drae.config :refer [drae-setting]]
            [drae.util.char :refer [char-replacer *greek-map*]]
            [drae.j.paxtools :refer :all]))

;;; Helper functions -- adjust bioentity names or find split-out-subnames
;;; -------------------------------------------------------------------------

(def ^{:private true} spell-out-greek-letters 
  "Replace Greek letters with their spelled-out equivalents."
  (char-replacer *greek-map*))

(defn- split-out-subnames
  "Given a composite bioentity name, such as YAP/TAZ, return a vector of the elements."
  [s]
  (clojure.string/split s #"\/"))

(def ^{:private true} bioentity-name-variants 
  (comp distinct (juxt identity #_lower-case #_upper-case spell-out-greek-letters))) ; Search already case-insensitive.

;;; Class methods
;;; -------------------------------------------------------------------------
(defn- model? "Is this a BioPax Paxtools model?" 
  [x] (instance? Model x))

(defn- pattern? "Is this a BioPax Paxtools pattern?" 
  [x] (instance? Pattern x))

(defn- xpattern? "Is this an XPattern wrapper?" 
  [x] (instance? XPattern x))

;;; Print methods
;;; -------------------------------------------------------------------------
(defmethod print-method org.biopax.paxtools.model.Model [x writer]
  (.write writer (format "<BioPax %s Model: %s (%d objs)>" (.getLevel x) (.getXmlBase x) (count (.getObjects x)))))

(defmethod print-method org.biopax.paxtools.model.level3.Named [x writer]
  (let [dispname (or (.getDisplayName x) (.getRDFId x) (str x))]
    (.write writer (format "<%s: %s>" (.getSimpleName (.getModelInterface x)) dispname))))

(defmethod print-method org.biopax.paxtools.pattern.MappedConst [x writer]
  (.write writer (format "<MappedConst: %s %s>" (.getSimpleName (class (.getConstr x)))
                         (into [] (.getInds x)))))



;;; PC2QUERY - Query the Pathway Commons 2 web service
;;; -------------------------------------------------------------------------

(def ^{:private true} +pathway-commons-address+ 
  "Address for the Pathway Commons 2 query service."
  "http://www.pathwaycommons.org/pc2/search.json")

(defn- pc-url [] +pathway-commons-address+) ;; Eventually replace with config file item. 

(defn- make-query-form [x] (str (pc-url) "?q=" x))

(defn- instances-named [coll entity-name]
  (filter #(.equalsIgnoreCase entity-name (:name %)) coll))

(defn- query-simple [entity-name]
  {:pre [(string? entity-name)]}
  (-> (or (:body (client/get (make-query-form entity-name)))
          (uerr "Unable to query Pathway Commons server for %s." entity-name))
    (json/read-str :key-fn keyword)
    :searchHit
    (instances-named entity-name)))

(defn pc2query 
   "Peform a Pathway Commons 2 search over the set of named entities. 
   `Names` can be a single string or a list of strings.
   If `:subnames?` is true (the default), will attempt to split the 
   strings into names (e.g., FOO/Bar ➔ Foo, Bar). 
   If `:variants?` is true, will try to create variants by 
   spelling out any Greek letters in the name. 
   Returns a map where each key is a protein name and the
   value is a collection of results."
   [names & {:keys [subnames? variants?]
            :or {subnames? true, variants? true}}]
   {:pre [(or (string? names) (coll? names))]
    :post [(map? %)]}
  (let [ns (if (coll? names) names (list names))]
   ; (println "NS is " ns " Subnames: " subnames? "Variants: " variants?)
    (cond 
      subnames? (pc2query (mapcat split-out-subnames ns)
                       :subnames? false, :variants? variants?)
      variants? (pc2query (mapcat bioentity-name-variants ns)
                       :subnames? subnames? :variants? false)
      :else 
        (into {} 
              (for [n ns :let [res (query-simple n)] :when (not (empty? res))] (vector n res))))
  
    ))


;;; Functions for reading BioPax models
;;; -------------------------------------------------------------------------

(defn- ontology-map "Map of available ontologies." []
  {:post [(map? %)]}
  (or (drae-setting :ontologies) 
      (uerr "Could not find :ontologies in DRAE setting files.")))

(defn- ontology-filename "Return the pathname of an ontology." [k] 
  {:pre [(keyword? k)], :post [(string? %)]}
  (if-let [m (get (ontology-map) k)]
   (:path m)
   (uerr "Could not find ontology %s. Available ontologies are: %s" k (keys (ontology-map)))))

(defn- read-biopax-ontology-direct 
  "Read an BioPax OWL file and return a new ontology model. `f` can be 
   a file, a filename (as a string), or a predefined ontology keyword.
   A list of all ontology keywords are printed when this function is called
   with an empty argument list."
  ([f]
    (println (format "Loading BioPax model from %s..." f))
    (let [real-file 
          (cond (file? f) f
                (string? f) (file f)
                (keyword? f) (ontology-filename f)
                :else (uerr "Could not read ontology %s." f))
          model 
          (time
            (with-open [instream (input-stream real-file)]
              (.. (SimpleIOHandler.) (convertFromOWL instream))))]
      (println (format " ...created model %s." model))
      model))
  ([]
    (println "Available ontologies:" (keys (ontology-map)))))

(def ^{:private true} read-biopax-ontology 
  "Read a BioPax OWL file and return a new ontology model. Memoized."
  (memoize read-biopax-ontology-direct))


;;; BIOENTITY-CLASS - Given a bioentity class name or symbol, return the class object (private)
;;; -------------------------------------------------------------------------------------------

(declare level3-elements) ;; Specified at end of file.

(defn- bioentity-classes-direct []
  (into {}
    (map #(vector (keyword (lower-case %)) (java.lang.Class/forName (str "org.biopax.paxtools.model.level3." %)))
         level3-elements)))

(def ^{:private true}  bioentity-classes (memoize bioentity-classes-direct))

(defn- available-bioentity-classes [] 
  (sort (map #(keyword (.getSimpleName %)) (vals (bioentity-classes)))))

(defn- plural-keyword? "Keyword ends in s?" [k] 
  (= \s (last (str k))))

(defn- plural? "String ends in s?" [s] (= \s (last s)))

(defn- deplural-if-needed "If ends in s, remove last letter." [s]
  (if (plural? s) (subs s 0 (dec (count s))) s))

(defn bioentity-class 
  "Return the BioPax bioentity class for the given name name, which
   should be a symbol, string, or keyword. With no
   arguments, returns all bioentity class keys."
  ([s]
    (let [class-table (bioentity-classes)
          name (cond (string? s) s 
                     (or (symbol? s) (keyword? s)) (name s)
                     :else (uerr "%s must be a bioentity-class string or symbol." s))
         classkey (-> name deplural-if-needed lower-case keyword)]
      (or (get class-table classkey)
          (uerr "Could not find bioentity class for %s." s))))
  ([] (available-bioentity-classes)))


;;; MODEL, MODEL! and MODELS functions.
;;; -------------------------------------------------------------------------

(defonce ^{:private true} +current-model+ #_"Current BioPax model (or nil for none)." (atom nil))

(defn ^Model model 
  "The BioPax model of the given name, which is loaded if 
   needed. When no argument, returns the current default model.

   Examples: 
 
     `(model :raf-cascade)` - Loads and returns a predefined model.
     `(model (file \"./resources/ontologies/RAF-Cascade.owl\"))` - Or a file pointer can be passed.
  "
  ([] @+current-model+) ; return default.
  ([name] 
    (cond (model? name) name ; Handle trivial case of being passed a model.
          :else (read-biopax-ontology name))))

(defn ^Model model! 
  "Same functionality as `model`, but also sets the model as the new default model."
  [name] (swap! +current-model+ (fn [_] (model name))))

(defn models 
  "A list of all the currently known (but not necessarily loaded) model names."
  [] (keys (ontology-map)))

;;; PATTERN, PATTERN! and PATTERNS functions.
;;; -------------------------------------------------------------------------

(defonce ^{:private true} +current-pattern+ #_"Current BioPax pattern (or nil for none)." (atom nil))

(defonce ^{:private true} +patterns+ #_"All patterns defined thus far, including defaults." (atom {}))

;(defn ^Pattern current-pattern [] "Current default BioPax search pattern." @+current-pattern+)

(defn patterns "List of all known pattern names." []
  (sort (keys @+patterns+)))

(defn pattern 
  ([pname]
    (or (get @+patterns+ pname)
        (uerr "Could not find a pattern named %s is known patterns: %s" pname (patterns))))
  ([] @+current-pattern+))
  
(defn pattern! "Set new default pattern." [pname] 
  (reset! +current-pattern+ (pattern pname)))

(defn index-pattern 
  "Index a pattern (actually an XPattern) in the +pattern+ map." 
  [xpat]
  (swap! +patterns+ assoc (symbol (:name xpat)) xpat))



;;; ALL - Function for selecting sets of entities.
;;; -------------------------------------------------------------------------------

(defn all
		"A set of all items in the model of the given class
		   and (optionally) name. If *in* is omitted, uses the default
   model. 

   Examples: 

   `(all :Protein)` ➔ All proteins in the current model. 

   `(all :Protein :in (model :raf-cascade) :named \"MEK2\")` 
    ➔ The set of MEK2 proteins in the RAF-Cascade Biopax ontology. 

   If *named* is a regular expression rather than a string or symbol, will perform
   a regular expression match:

   `(all :Protein :named #\"MEK\\d\")`

    ➔ All proteins called MEK with one digit (e.g., MEK1, MEK2).
  " 
  [classkey & {:keys [in named]}]
  (let [model (or in (model) (uerr "No model specified."))
        items (set (.getObjects model (bioentity-class classkey)))
        match-fn (cond 
                   (nil? named) nil
                   (symbol? named) #(.equalsIgnoreCase (str named) (.getDisplayName %))
                   (string? named) #(.equalsIgnoreCase named (.getDisplayName %))
                   (instance? java.util.regex.Pattern named)
                      #(re-matches named (.getDisplayName %))
                   :else (uerr "Can't match against named argument %s." named))]
    (if-not (nil? named)
       (filter match-fn items)
       items)))


;;; ALIASES - Alternative names.
;;; -------------------------------------------------------------------------------

(defn aliases 
  "Set of all alternate names for a single entity or an entity set. 

   Examples: 
   
   `(aliases (all :Protein))` ➔ The set of all names of all proteins in the current model.   

   `(aliases (all :Protein :named \"MEK\"))` ➔ Alternative names for the MEK protein. 
  "
  [e]
  (let [l (if (coll? e) e (list e))] 
    ;; Note - .addAll is destructive, so create new hashset and loop instead of map.
    (let [res (java.util.HashSet.)]
      (doseq [hs (map (fn [^Named x] (.getName x)) l)]
        (.addAll res hs))
      res)))

(defn common-generic-references 
  "Attempt to find a single common generic reference for all the entities 
   in the list. If none, if more than one, return nil."
  [entity-list]
  (distinct (mapcat #(.getGenericEntityReferences %) entity-list)))

(defn find-protein-references 
  ([prot-name model]
    (let [singleton? #(and (first %) (not (rest %)))
          pro-refs (all :ProteinReference :named prot-name :in model)]
      (if (singleton? pro-refs)
        (first pro-refs)
        (let [proteins (all :Protein :named prot-name :in model)]
          (common-generic-references proteins)))))
  ([prot-name] (find-protein-references (model) prot-name)))
    
(defn protein-reference-map [prot-name-list]
  (into {} (map #(vector % (find-protein-references %)) prot-name-list)))

;;; DEFPATTERN -- Define new patterns.
;;; -------------------------------------------------------------------------

(def ^{:private true} pattern-constraints 
  '{:not-equal   (. ConBox equal false)
    :referenced-protein  (. ConBox erToPE)
    :controlled-by  (. ConBox peToControlledConv)
    :new-output-node (Participant. RelType/OUTPUT)
    :new-input-node (ParticipatesInConv. RelType/OUTPUT)
    :controller-for (. ConBox convToController)
    :protein-reference-for (. ConBox peToER) 
    })

(defn- lookup-pattern-method [meth]
  (cond 
    (list? meth) meth     ;; Function call -- don't change it.
    (keyword? meth)       ;; Preset method, pull from table.
      (or (get pattern-constraints meth)
          (uerr "Could not find pattern method for %s." meth))
    (symbol? meth)        ;; Single symbol - treat as ConBox method.
      `(. ConBox ~meth)
    :else
       (uerr "Could not determine proper pattern method: %s." meth)))

(defn- add-constraint 
  ([pat [meth & args]]
    (let [constraint (lookup-pattern-method meth)]
      `(.add ~pat ~constraint (into-array String '~(map str args))))))

(defn- add-constraints [pat-name constraints]
  ;; Simply returns the contraint values for defpattern.
  (map #(add-constraint pat-name %) constraints))

(defmacro defpattern [name [source source-bioclass,
                            target target-bioclass] doc-str & constraints]
  (let [source-class (bioentity-class source-bioclass)
        target-class (bioentity-class target-bioclass)
        pat (gensym 'pat)]
    `(let [~pat (Pattern. ~source-class ~(str source))
           arg-map# '{:source ~(str source) :source-class ~source-class
                      :target ~(str target) :target-class ~target-class}]
       ~@(add-constraints pat constraints)
       (doto 
         (->XPattern (str '~name) ~pat arg-map# ~doc-str)
         index-pattern)
       )))
 
(defn- expand-patternbox-pattern 
  "Given a method name, argument description and doc string, return an XPattern
   object for that PatternBox predefined patter."
  [name [source source-class, target target-class] doc-str]
  `(doto 
     (->XPattern '~name (. PatternBox ~name)
         '{:source ~(str source) :source-class ~(bioentity-class source-class)
           :target ~(str target) :target-class ~(bioentity-class target-class)}
         ~doc-str)
     index-pattern))
  
(defmacro patternbox-pattern [name args doc-str]
  (expand-patternbox-pattern name args doc-str))

(defmacro patternbox-patterns [& pattern-list]
  `(list ~@(for [[name args doc-str] pattern-list] (expand-patternbox-pattern name args doc-str))))

;;; MATCHES



;;; SEARCH
;;; -------------------------------------------------------------------------
(defn search 
  "Search a model with the given pattern. If no argument is given, use the default pattern
   and model (set using `pattern!` and `model!` functions. Otherwise, the pattern is the 
   first argument. Optional keyword argument are `:in <model>` and `:starting-with <entity collection>'.

   Returns a collection of xmatches."
  ([pat & {:keys [in starting-with]}]
    (let [model (or in (model))
          search-pat (if (instance? Pattern pat) pat (:pattern pat))
          result
          (cond 
            (nil? starting-with) (Searcher/search model search-pat)
            :else (Searcher/search starting-with search-pat))]
      ;; Wrap result in XMatches
      (map (fn [[k v]] (->XMatch k (into [] v) pat)) result)))
  ([] (search (pattern))))

;;; DESCRIBE -- Verbose description of models, entities, patterns, and matches.
;;; ----------------------------------------------------------------------------
(defmulti describe 
  "Describe a BioPax instance. Argument can be a model, entities, patterns, or 
   matches (from search)." 
  class)

(defmethod describe Model [m]
  (let [>> (fn [& x] (println (apply format x)))
        myname #(with-out-str (print-method % *out*))]
    (>> "%s is a MODEL." (myname m))
    ))

(defmethod describe Pattern [p]
  (let [>> (fn [& x] (println (apply format x)))
        myname #(with-out-str (print-method % *out*))
        constraints (.getConstraints p)
        size (count constraints)]
    (>> "%s is a PATTERN containing %d constraint%s:" 
        p size (if (< 1 size) "s" ""))
    (>> "  Starting class: %s" (.. p getStartingClass getSimpleName))
    (doseq [c (.getConstraints p)]
      (>> "   - %s" (myname c)))
    )
  )

(defmethod describe drae.j.paxtools.XPattern [xp]
  (let [>> (fn [& x] (println (apply format x)))
      myname #(with-out-str (print-method % *out*))
    ;  constraints (.getConstraints p)
    ;  size (count constraints)
       {:keys [source target source-class target-class]} (.args xp)
      ]
  (>> "%s is an XPATTERN from a source %s (%s) to a target %s (%s):"
      (.name xp) (.getSimpleName source-class) source (.getSimpleName target-class) target)
;  (>> "  Starting class: %s" (.. p getStartingClass getSimpleName))
  (>> (.explain-fmt xp) source target)
  ))


;;; Indexing for search
;;; -------------------------------------------------------------------------

(declare biopax-generic-entity-map) ; See below.

(def +names-index-by-model+ (atom {}))

(defn names-index
  "Return the names index (for bioentity classification) for the given model, 
   generating it first if needed. Once generated, it is stored and the same index
   is returned when the function is passed the same model."
  ([model]
    (or (get @+names-index-by-model+ model)
        (do (println (format "Creating names index for %s..." model))
          (let [new-index 
                (into {} ; new index
                      (map #(vector % (set (map lower-case (aliases (all % :in model))))) 
                           (map keyword (keys biopax-generic-entity-map))))]
            (swap! +names-index-by-model+ assoc model new-index)
            (println (format "  ...finished names index for %s." model))
            new-index))))
  ([] (names-index (model))))

(defn find-name-class 
  ([name model]
    (let [names-index (names-index model)
          lcname (lower-case name)]
      (for [k (keys names-index) :when (contains? (get names-index k) lcname)]
        k)))
  ([name] (find-name-class name (model))))

;;; Find one or more elements by name.
(defn find-bioentity 
  ([namestring model]
    (let [classes (find-name-class namestring model)]
      (mapcat (fn [c] (all c :named namestring :in model)) classes)))
  ([namestring] (find-bioentity namestring (model))))
                  

;;; Constants
;;; -------------------------------------------------------------------------

(def ^{:private true} level3-elements 
  '[BindingFeature, BiochemicalPathwayStep, BiochemicalReaction, BioSource, Catalysis, 
    #_CellularLocationVocabulary, #_CellVocabulary, #_ChemicalConstant, ChemicalStructure, 
    Complex, ComplexAssembly, Control, ControlledVocabulary, Controller, Conversion, 
    CovalentBindingFeature, Degradation, DeltaG, Dna, DnaReference, DnaRegion, 
    DnaRegionReference, Entity, EntityFeature, EntityReference, 
    EntityReferenceTypeVocabulary, Evidence, EvidenceCodeVocabulary, ExperimentalForm, 
    ExperimentalFormVocabulary, FragmentFeature, Gene, GeneticInteraction, 
    Interaction, InteractionVocabulary, KPrime, ModificationFeature, Modulation, 
    MolecularInteraction, #_Named, NucleicAcid, NucleicAcidReference, 
    NucleicAcidRegionReference, Observable, Pathway, PathwayStep, #_PhenotypeVocabulary, 
    PhysicalEntity, Process, Protein, ProteinReference, Provenance, PublicationXref, 
    RelationshipTypeVocabulary, RelationshipXref, Rna, RnaReference, RnaRegion, 
    RnaRegionReference, #_Score, SequenceEntityReference, SequenceInterval, SequenceLocation, 
    SequenceModificationVocabulary, SequenceRegionVocabulary, SequenceSite, SimplePhysicalEntity, 
    SmallMolecule, SmallMoleculeReference, Stoichiometry, TemplateReaction, 
    TemplateReactionRegulation, TissueVocabulary, Transport, 
    TransportWithBiochemicalReaction, UnificationXref, UtilityClass, Xref, XReferrable
    ])

(defn simple-entity? "Is this a simple physical entity with a generic reference?" [x] 
  (instance? SimplePhysicalEntity x))

(def biopax-generic-entity-map
  '{Dna DnaReference,
    DnaRegion DnaRegionReference,
    NucleicAcid NucleicAcidReference,
    Protein ProteinReference,
    Rna RnaReference,
    RnaRegion RnaRegionReference,
    SmallMolecule SmallMoleculeReference
    })

;; Stock the set of patternbox patterns.

(patternbox-patterns
  
  (bindsTo [?pr1 :ProteinReference, ?pr2 :ProteinReference] 
           "Protein %s binds to protein %s.") 
  

  (controlsStateChange ["controller PR" :ProteinReference, "changed PR" :ProteinReference]
      "Protein %s has a member that controls a state change reaction of %s.")
  
  (controlsTransport ["controller PR" :ProteinReference, "changed PR" :ProteinReference]
      "Protein %s has a member that controls a transportation of %s.")
  
  (inSameActiveComplex [?er1 :EntityReference, ?er2 :EntityReference]
      "Entity references %s and %s are in the same active complex.")
  
  (inSameComplex [?er1 :EntityReference, ?er2 :EntityReference]
      "Entity references %s and %s are in the same complex.")
  
  (molecularInteraction [?pr1 :ProteinReference, ?pr2 :ProteinReference]
      "Proteins %s and %s are related through a molecular interaction.")
  
  (neighborOf [?pr1 :ProteinReference, ?p2 :ProteinReference]
      "Proteins %s and %s are related through an interaction, either as participants or controllers.")
    
  )

:DONE ;; 


