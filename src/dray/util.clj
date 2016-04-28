;;;; Name:       dray.util
;;;; Purpose:    Utility code.
;;;; Written by: Ron Ferguson, Leidos
;;;; Created:    2014-04-26
;;;; Updated:    2014-05-08
;;;;
(ns dray.util
  "Utility routines used by DRAY. These routines are not part of the API, and may change, but
   are documented here for reference."
  (:import (java.io File)
           (java.net URL)
           (java.nio.file Path)
           (java.lang String)
           (java.rmi.dgc VMID))
  (:require [clojure.java.io :refer [file as-url]]
            [clojure.set :refer [intersection]]
            [clojure.string :as s]) 
  )

;;; DEFCONST -- DEFINING SINGLE OR MULTIPLE CONSTANT VALUES.

(defn- expand-defconst [[name val]]
  `(def ~(with-meta name (assoc (meta name) :const true)) ~val))

(defn- expand-defconsts [name-value-pairs]
  (map expand-defconst (partition 2 name-value-pairs)))

(defmacro defconst
  "Define one or more constants, given as a list of name / value pairs."
  [constant-name assigned-value & additional-pairs]
  (cons 'do
        (expand-defconsts `(~constant-name ~assigned-value ~@additional-pairs))))

(defn java-version "Current Java version as string." []
  (java.lang.System/getProperty "java.version"))

;;; TESTING COMPARATORS

(defn expand-map-selector-fn [xvar args]
  (let [make-key-selector-pair (fn [[k sel]] `(~k ((memfn ~sel) ~xvar)))]
    `(hash-map ~@(mapcat make-key-selector-pair (partition 2 args)))))

(defmacro map-selector-fn 
  "Given an alternating list of keywords and Java methods, returns
   a function that, when passed a Java object, returns a hashmap
   of the keys and the results of the selectors on the given object.
   Example: (map-selector-fn :name getName :parent getParent)."
  [& key-selector-args]
  (let [xvar `x#] ; hygenic
    `(fn [~xvar] ~(expand-map-selector-fn xvar key-selector-args))))

(defn file-type? 
  "Returns true if a file has the given extension."
  [a-file extension]
  (.endsWith (.getName a-file) extension))

;;;; ERROR MESSAGES

(defmacro uerror 
  "User error message. Takes the arguments and passes then to `str` to create a general Exception with that message. 
   Depracated in favor of uerr."
  [& msg-args]
  `(throw (Exception. (str "- " ~@msg-args))))

(defmacro error-when 
  "If the given expression evaluates to true, signal a general error with the given arguments, which are
   passed to `str`."
  [cond-exp & msg-args]
  `(when ~cond-exp (uerror ~@msg-args)))
  
(defmacro uerr 
  "Takes an optional Exception class, a format string, and a set of arguments. Will throw
   the exception and print out the error message."
  [& args]
  (let [first-arg-string? (string? (first args))
        e (if first-arg-string? java.lang.Exception (first args))
        fmt (if first-arg-string? (first args) (second args))
        fmt-args (if first-arg-string? (rest args) (nthrest args 2))]
    `(throw (new ~e (format ~fmt ~@fmt-args)))))


;;; CAMELCASE CONVERSION

(defn hyphenate-camelcase [camelcase-string]
  (s/lower-case (s/replace camelcase-string #"([a-z])([A-Z])" "$1-$2")))

(defn hyphenate-classname [class] (hyphenate-camelcase (.getSimpleName class)))


;;; KEYWORDS

(def de-keyword 
  "Remove colon from front of keyword, returning regular symbol."
  (comp symbol name))


;;; CLASS NAMES

(defn namespace? "Is this a namespace?" 
  [x] (instance? clojure.lang.Namespace x))

(defn full-classname-for-symbol
  "Given a single symbol representing a classname, attempt to find the full
   classname reference in the given namespace. Uses the current namespace 
   (*ns*) if none is given. Can also accept a namespace instance instead
   of a namespace name."
  ([class-symbol ns-symbol]
    (let [the-ns (if (namespace? ns-symbol) ns-symbol (find-ns ns-symbol))
          found-class (ns-resolve the-ns class-symbol)] 
      (when-not found-class (uerr "Could not find full classname for symbol %s." class-symbol))
      (.getCanonicalName found-class)))
  ([class-symbol] (full-classname-for-symbol class-symbol *ns*)))
     

;;; RETRIEVING PRIVATE FUNCTIONS FOR TESTING

(defmacro private-function 
  "Retrieves the function from a namespace, even if it is internal. Nil if not
   found. This function is useful for testing functions that are kept private
   (e.g., an internal function that is important to test, but not part of the API."
  [name ns]
  `(let [name# '~name
         ns# '~ns
         result# (get (ns-interns (find-ns ns#)) name#)]
     (or result# (uerr "Could not find private function %s in namespace %s." name# ns#))))
       
;;; CREATE A FUNCTION REPRESENTING A PROTECTED FIELD

(defn protected-field-fn 
  "Returns a function that get the value of a protected field."
  [class fieldname]
  (let [field 
        (try (.getDeclaredField class fieldname)
          (catch Exception e (uerr "protected-field-fn - could not find field %s in %s: %s"
                                  fieldname class e)))]
    (.setAccessible field true)
    #(.get field %)
  ))

#_(defn protected-method-fn 
   "Returns a funtion that calls a protected method for the given class."
   [class methodname args]
   (let [method 
         (try (.getDeclaredMethod class methodname)
           (catch Exception e (uerr "protected-method-fn - could not find method %s in %s: %s"
                                    methodname class e)))]
     (.setAccessible method true)
     method
     ))

  
;;; DELETE A DIRECTORY

(defn delete-directory 
  "Delete a directory, even if it contains files. Use with caution.
   Verbose by default."
  ([dir verbose?]
    (doseq [f (reverse (file-seq dir))] ;; Leaves first.
      (if verbose? (print "Deleting " f "\n"))
      (let [res
            (try (.delete f)
              (catch Exception e 
                (println (str "Could not delete " f " " (.getMessage e)))))]
        (when-not res 
          (println "Could not delete directory " dir))
      )))
  ([dir] (delete-directory dir true)))

;; PATHS

(defn path-elements "Names constituting the file's path, in order." [f]
  (map str (iterator-seq (.iterator (.toPath f)))))

;; CACHE DIRECTORY

(defonce current-cache-dirs #_"Current set of cache directory files." (atom #{}))
  
(defn retrieve-current-cache-dirs [] 
 ; (println "Current cache dirs are " @current-cache-dirs)
  @current-cache-dirs)

(defn delete-current-cache-dirs 
  "Delete all the cache directories created or referenced during this session thus far."
  []
  ; (println "Starting to delete cache dirs.")
  (let [cache-dirs (retrieve-current-cache-dirs)]
    ; (println "Cache dirs are: " cache-dirs)
    (doall (for [d cache-dirs]
             (do 
               (println "Deleting cache directory: " d)
               (delete-directory d)))))
  true) ; Always returns true (for now).
  
(defn make-dirs "If directory f does not exist, create it. Returns f."
  [f]
  (when-not (.exists f) (.mkdirs f))
  f)

(defn cache-directory 
  "The DRAY cache directory for a given file. Always called .dray-cache in the 
   same directory as the file itself. If cache directory does not exist, creates
   it."
  [f]
  (let [cache-dir (file (str (.getParent f) "/.dray-cache"))]
    (make-dirs cache-dir)
    (swap! current-cache-dirs conj cache-dir) ;; Add to list of cache directories.
    (.deleteOnExit cache-dir) ;; When JVM exits, remove dray-cache.
    cache-dir))

(defn rootname 
  "Returns the name of the file without the containing directory or
   the extension."
  [file] (#(subs % 0 (- (count %) 4)) (.getName file)))

(defn cache-file-for 
  "Return a specific cache file for this PDF, using the prefix, root-name of the PDF, and the suffix."
  [prefix suffix pdf]
  (let [rootname (rootname pdf)]
    (file (cache-directory pdf) (str prefix rootname suffix))))

(defn clear-dray-cache-for 
  "Remove all cache files for this PDF's cache (which includes the cache files for all sibling PDFs as well)."
  [input-pdf]
  (delete-directory (cache-directory input-pdf)))  
 
(defn resolve-cache-file-for 
  "Given a PDF file and another path (as a file), merge the other path
   with the cache directory for the given PDF file. Attempts to account 
   for overlapping path elements."
  [pdf other-path]
  (let [cache-dir (cache-directory pdf)
        other-path-elements (path-elements other-path)
        cache-dir-name? #(= ".dray-cache" %)]
    (cond
      (.startsWith (.getPath other-path) (.getPath cache-dir)) 
         other-path
      (some cache-dir-name? other-path-elements)
         (apply file cache-dir (rest (drop-while (complement cache-dir-name?) other-path-elements)))
      :else 
         (file cache-dir other-path))))

;;; TYPE COOERCION

(defn ->url 
  "Cooerce the argument, which must be a file or a string (assumed to be
   a filename), to a URL."
  [x]
  (cond (instance? URL x) x
        (instance? File x) (as-url x)
        (instance? String x) (as-url (file x))
        :else #_(uerror "Could not coorce " x " into a URL.") ; 2014-10-23
        (uerr IllegalArgumentException "Could not coorce %s into a URL" x)
        ))

(defn ->filename
  "Coorce the argument, assumed to be a file or a filename string, into a 
   filename string."
  [x]
  (cond (instance? String x) x
        (instance? File x) (.getPath x)
        :else #_(uerror "Could not coorce " x " into a filename string") ; 2014-10-23
          (uerr IllegalArgumentException "Could not coerce %s into a filename string." x)))

(def ->file "Coorce argument into a file." file)
              
(defn file? "Is this a file?" [x] (instance? java.io.File x))

;;; CLASS HIERARCHIES

(defn ordered-ancestors 
 "Ordered list of class ancestors for this class, most specific first."
 [c]
 (let [sup (.getSuperclass c)]
   (if (nil? sup) '() (cons sup (ordered-ancestors sup)))))

(defn common-ancestors [c1 c2]
  (intersection (set (ordered-ancestors c1)) (set (ordered-ancestors c2))))

;;; SEQUENCE UTILITIES

(defn every-other [s] (if (empty? s) s (cons (first s) (every-other (nthrest s 2)))))

(defn pairwise-group-by [s test-fn]
  {:pre [(coll? s)]}
  (let [good-partition? (fn [[g]] (test-fn (first g) (second g))) ; Partition meets test?
        partitions (partition-by (fn [[a b]] (test-fn a b)) (map vector s (rest s)))]
    (map (fn [res] (cons (first (first res)) (map second res))) 
         (every-other 
           (if (good-partition? (first partitions)) partitions (rest partitions))))))

(defn pairwise-split-when 
  "Split the given collection at the point between the pair of items
   that meet the given test. For example, given a test of =, for
   (1 2 3 3 4) return [[1 2 3] (3 4)]."
  [f coll]
  (loop [head '[], tail coll]
    (let [[a b & r] tail]
      #_(println (format "head: %s tail: %s a: %s b: %s" head tail a b))
      (cond 
        (nil? b) [(if a (conj head a) head) '[]]
        (f a b)  [(conj head a) (rest tail)]
        :else (recur (conj head a) (rest tail))))))
      
(defn pairwise-partition-when 
  "Just like pairwise-split-when, but returns a partitioning instead of
   a single split. For example, (pairwise-partition-when = '(1 2 2 3 4 4 5)) 
   should return [[1 2] [2 3 4] [4 5]]."
  [f coll]
  (loop [so-far '[], remaining coll]
    (let [[head tail] (pairwise-split-when f remaining)]
      (cond
        (empty? tail) (conj so-far head) ; done.
        :else (recur (conj so-far head) tail)))))

(defn pairwise-replace 
  "Runs the given *test-fn* on adjacent pairs of items in sequence *s*. When the test is true,
   replaces the pair of items with a new item created by passing those two items to *merge-fn*. 
   The replacement item will then be tested against the next item in the sequence, so that
   multiple merges may take place.

   Does not currently handle nil item values well.

   Example: Replace pairs of equal numbers with their sum:

   `(pairwise-replace '(1 2 2 3 4 5 5 10 11) = +)` ; Returns (1 4 3 4 20 11). Note that 5, 5 = 10 + 10 = 20.

  " 
  [s test-fn merge-fn]
  (loop [[a b & r :as all] s, res (list)]
    #_(println (format "A: %s B: %s Res: %s" a b res))
    (cond (empty? all) all 
          (empty? (rest all)) (reverse (cons a res))
          (test-fn a b) (recur (cons (merge-fn a b) r) res)
          :else (recur (cons b r) (cons a res)))))
  

(defn instances "Return all instances of a class from given collection." [c coll]
  (filter #(instance? c %) coll))

(defn decode-url "Given a URL string, replace the encoded elements with the original
                  string elements -- e.g., %20 -> space."
  [url-string]
  (java.net.URLDecoder/decode url-string))

(defn meta-seq 
  "Create a meta-sequence by running the given function on all sequential pairs of the sequence."
  [fn s]
  (map fn s (rest s)))
