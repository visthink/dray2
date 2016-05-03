(ns dray.j.Producer
  "A set of testing (toy) routines for system development."
  (:import  (java.util ArrayList Map)
            (java.lang String))
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.walk :refer [prewalk]]
            [seesaw.invoke :refer [invoke-now]]
            [seesaw.chooser :refer [choose-file]]
            #_[dray.gui :refer [gui-save-map-as-json]]
            [dray.util :refer [uerr]])
  )

(gen-class :name dray.j.Producer.Table
           ; :state state  
           ; :prefix "bbox-"
           :methods  
               [^{:static true} [allWSProducers [] java.util.List]
                ^{:static true} [applyWSProducer [java.lang.String java.lang.Object] java.lang.Object]
                ^{:static true} [allLayerProducers [] java.util.List]
                ^{:static true} [applyLayerProducer [java.lang.String java.lang.Object] java.lang.Object]
                ^{:static true} [getWSProducer [java.lang.String] java.util.Map]
                ^{:static true} [getLayerProducer [java.lang.String] java.util.Map]
                ^{:static true} [layerRepToJSON [java.lang.Object] java.lang.String]
                ^{:static true} [layerToJSONString [java.lang.Object] java.lang.String] 
                ^{:static true} [stringToKeyword [java.lang.String] clojure.lang.Keyword]
                ^{:static true} [parseProducerList [java.lang.String] java.util.ArrayList]
                ]
               
            )

(def ^:private WSProducerTable "All available producers for working sets." (atom {}))

(def ^:private LayerProducerTable "All available producers for layers." (atom {}))

(defrecord Entry [key name doc fn]
  Object
  (toString [this] (format "<Producer.Entry %s %s>" (.key this) (.fn this))))

(defn- ->namestring
  "Helper function - converts symbols or keywords to their string. Leaves strings alone."
  [x]
  (if (or (keyword? x) (symbol? x))
    (.getName x)
    x))

(defn make-producer 
  "Create a single producer entry object. Key should be either a keyword, string, or symbol.
   If the key is omitted, uses the name as the key (this is for backward compatibility.
   Producer entries can be accessed via the .key, .name, .doc, and .fn methods."
  ([key name doc fn]
    (let [real-key (->namestring key)] 
      (Entry. real-key name doc fn)))
  ([name doc fn] (make-producer name name doc fn)))
  
(defmethod print-method Entry [obj ^java.io.Writer w]
  (.write w (format "<Producer.Entry %s fn: %s>" (.key obj) (.fn obj))))
    
;;; add-producer set

(defn- add-producer
  "Add a producer entry to the producer table, replacing old entry if needed. Returns nil."
  ([mykey myname doc fn table]
    (let [realkey (->namestring mykey)]
      (swap! table
             #(-> %
                (dissoc realkey) ; Remove old.
                (assoc realkey (make-producer realkey myname doc fn)))))) ; Add new.
  ([mykey myname doc fn] (add-producer mykey myname doc fn WSProducerTable)))

(defn add-ws-producer 
  "Add a working set producer entry." 
  [mykey myname doc fn]
  (add-producer mykey myname doc fn WSProducerTable))

(defn add-layer-producer "Add a layer producer." [key name doc fn] (add-producer key name doc fn LayerProducerTable))

;;; all-producers set

(defn- all-producers 
  "Returns a list of all known producer entries as an array list." 
  [table] 
  (if-not (empty? @table)
    (ArrayList. (vals @table))
    (ArrayList. '())))

(defn all-ws-producers 
  "All known producer entries applicable to working sets as an ArrayList of producers." 
  [] 
  (all-producers WSProducerTable))

(defn all-layer-producers 
  "All known producer entries applicable to layers as an ArrayList of producers." 
  [] 
  (all-producers LayerProducerTable))

(def -allWSProducers "Java equivalent of all-ws-producers" all-ws-producers)

(def -allLayerProducers "Java equivalent of all-layer-producers." all-layer-producers)

;;; get-producer 

(defn- get-producer 
  [mykey table] 
  (or (get @table (->namestring mykey))
      (uerr "Could not find producer with key %s" mykey)))

(defn get-ws-producer 
  "Returns working set producer entry with the given key." 
  [mykey] (get-producer mykey WSProducerTable))

(defn get-layer-producer 
  "Returns layer producer entry with the given name." 
  [mykey] (get-producer mykey LayerProducerTable))

(def -getWSProducer "Java version of get-ws-producer." get-ws-producer)

(def -getLayerProducer "Java version of get-layer-producer." get-layer-producer)

;;; apply-producer

(defn- apply-producer [name obj table] ((:fn (get-producer name table)) obj))

(defn apply-ws-producer 
  "Apply producer of the given key to the object (a working set)." 
  [key obj] 
  (apply-producer key obj WSProducerTable))

(defn apply-layer-producer 
  "Apply producer of the given key to the object (a layer)."
  [key obj] 
  (apply-producer key obj LayerProducerTable))

(def -applyWSProducer "Java equivalent of apply-producer." apply-ws-producer)

(def -applyLayerProducer "Java equivalent to apply-layer-producer." apply-layer-producer)

(defn gui-get-save-filename []
  (invoke-now 
    (choose-file #_(.getFrame (current-gui)) :type :save)))

(defn layer-as-JSON-string 
  "Return a JSON value as a formatted string."
  [m]
  (with-out-str (json/pprint m)))
    
(defn gui-save-map-as-json 
  "Save the given map (or other json-writable form) as a JSON file."
 [m]
 (let [filename (gui-get-save-filename )]
   (when filename
     (let [res (with-out-str (json/pprint m))]
       (spit filename res)))))

(defn -layerToJSONString "Given a hashmap, return the hashmap as a JSON representation string."
  [layer-rep]
  (layer-as-JSON-string layer-rep))

(defn -layerRepToJSON "Given a hashmap, return the hashmap as a JSON representation string."
  [layer-rep]
  (println "Producing a JSON output.")
  (println "  Class of layer-rep: " (class layer-rep))
  (println "  Class of car of layer-rep: " (class (first layer-rep)))
  
  ;; (with-out-str (json/pprint layer-rep)))
  (gui-save-map-as-json layer-rep))

(defn -stringToKeyword [s] (keyword s))

(defn -parseProducerList 
  "Parse the input string, which is a Clojure array of arrays expression,
   and return the array of arrays."
  [s]
  (let [stringify-fn #(if (symbol? %) (str %) %)
        result (prewalk stringify-fn (edn/read-string s))] ;; Parse, then replace symbols with strings.
    ;; If array-of-arrays, we're good.
    ;; If array of strings, then we need to nest one more level.
    ;; No error checking at this point, alas.
    (cond
      (string? (first result))         ;; One level deep, make into two-level deep singleton.
        (ArrayList. (list (ArrayList. result)))
      (coll? (first result))           ;; Two level deep, keep as array of arrays.
        (ArrayList. (map #(ArrayList. %) result)) 
      :else 
        (uerr "Could not parse producer string of %s." s))))


