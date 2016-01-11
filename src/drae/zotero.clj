(ns drae.zotero
  "This package is a new version of the corpus tool."
  (:require [clj-http.lite.client :as client]
            [slingshot.slingshot :refer [try+ throw+]]
            [clojure.string :refer [split-lines]]
            [clojure.data.json :as json]
            [clojure.walk :refer [keywordize-keys]]
            [drae.util :refer [uerr]]
            ))
(def +zotero-base+ "https://api.zotero.org")

(def +zlib+ "Current Zotero library, if any." (atom nil))

(def +libs+ 
  {:cancer-mirna
   {:name "Cancer miRNA"
    :type 'group
    :id 46360
    }
   :bm-kinetics
   {:name "Big Mechanism Kinetics"
    :type 'group
    :id 346164
    :api-key "zGQwsIHMTkmBaCcw9kaUQFM9"
   }
   :bm-clinical
   {:name "Big Mechanism Clinical"
    :type 'group
    :id 347043
    :api-key "lUkX0ENz14OR3gwCndehE7hv"
    }
   :bm-high
   {:name "Big Mechanism High-Throughput"
   :type 'group
   :id 347748
   :api-key "keid14XFca22RNRA6TbvVlT2"
   }
   :csl-test
   {:name "CSL styles development"
    :type 'group
    :id 4211
    }
  }
 )


(defn zlibs "Available Zotero library presets." [] (keys +libs+))


(defn zlib 
  "Current Zotero library preset (if any), or Zotero library preset if keyword." 
  ([] @+zlib+)
  ([preset-keyword-or-model] 
    (cond (keyword? preset-keyword-or-model) (get +libs+ preset-keyword-or-model)
          (map? preset-keyword-or-model) preset-keyword-or-model
          :else (uerr "Don't know how to retrieve a Zotero library preset for %s" preset-keyword-or-model))))
      
          
(defn zlib! 
  "Set the current Zotero library (for use with other routines)."
  [new-zlib]
  (reset! +zlib+ (zlib new-zlib)))


;; UTILITY FUNCTIONS

(defn- ->string "Turn a keyword or symbol into a string."
  [x]
  (cond (or (symbol? x) (keyword? x)) (.getName x)
        (string? x) x
        :else (uerr "%s must be a string, symbol, or keyword.")))

(defn make-commands [commands]
  (apply str (doall (interpose "/" (map ->string commands)))))


(defn make-zquery-string 
  "Create a query string."
  ([lib commands #_params]
    (str
      (format "%s/%ss/%s/%s" +zotero-base+ (:type lib) (:id lib) 
              (make-commands commands))
      (if-let [api-key (:api-key lib)]
        (format "?key=%s" api-key)
        "")))
  ([commands]
    (make-zquery-string @+zlib+ commands)))

(defn zquery
  "Perform a Zotero query with the given library and command.
   If only the command is given, uses the default Zotero 
   library." 
  ([lib command] 
    (when (nil? lib)
      (uerr "No Zotero library given for command %s" command))
    (let [query-string (make-zquery-string lib command)]
      (->
        (try+
          (client/get query-string {:as :json, #_:headers #_hdrs})
          (catch Object _
            (uerr "Unable to perform GET using query string: \"%s\"" query-string)))
        :body
        json/read-str
        keywordize-keys)))
  ([command]
    (zquery @+zlib+ command)))

(defn zquery-keys 
  "Perform the same sorts of queries as zquery, but return the results
   an an array of item key strings."
  ([lib command]
    (when (nil? lib)
      (uerr "No Zotero library specified."))
    (let [query-string (str (make-zquery-string (zlib lib) command) "&format=keys")]
      (->
        (try+
          (client/get query-string)
          (catch Object _
            (uerr "Unable to perform GET using query string: \"%s\"" query-string)))
        :body
        clojure.string/split-lines
        ))))

(defn get-zitem
  "Given the key, return the item."
  ([lib key]
    (zquery (zlib lib) (list "items" key)))
  ([key]
    (get-zitem (zlib) key)))

(defn get-zitem-children 
  "Given the key, return the keys of the child items."
  ([lib key]
    (zquery-keys (zlib lib) (list "items" key "children")))
  ([key]
    (get-zitem-children (zlib) key)))

(defn item-title [item] (get-in item [:data :title]))

(defn item-type [item] (get-in item [:data :itemType]))

(defn item-filename [item] (get-in item [:data :filename]))

;; Helper function to get the file extension.
;;
(defn file-ext [fname]
  (let [ext-start (.lastIndexOf fname ".")]
    (if (pos? ext-start)
      (subs fname (inc ext-start) (count fname))
      "")))

(defn item-filetype [item]
  (if-let [fname (item-filename item)]
    (file-ext fname)
    ""))

(defn item-content-type [item]
  (let [content (get-in item [:data :contentType])]
    (if-not (empty? content)
      content
      (item-filetype item))))
      
(defn item-link-mode [item]
  (get-in item [:data :linkMode]))

(defn item-parent [item] (get-in item [:data :parentItem]))

(defn attachment? [item] (= (item-type item) "attachment"))

(defn article? [item] (= (item-type item) "journalArticle"))

;;;; Specific commands

(defn zlib-items 
  "Return a list of all items in the Zotero library."
  ([zlib]
    (zquery zlib '("items" "top")))
  ([] (zlib-items (zlib))))

(defn zlib-item-keys 
  "Return a list of all item keys at the top level of the Zotero library."
  ([zlib]
    (zquery-keys zlib '("items" "top")))
  ([] (zlib-item-keys (zlib))))
