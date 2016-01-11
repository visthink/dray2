(ns drae.domain.domains
  "Facilities for creating recognizer test domains for recognizing column, row, and element types."
  (:require [drae.util :refer [uerr]])
  )

;;; Domain record definition
;;; -------------------------------------------------------------------------

(defrecord Domain [name subdomains recognizer-tests doc]
  Object
  (toString [this] (format "<Domain: %s>" (:name this))))

(defmethod print-method Domain [x writer]
  (.write writer (.toString x)))


;;; Maintain global table of Domains.
;;; -------------------------------------------------------------------------

(def +domains+ "Map of current domains keyed by domain name." (atom {}))

(defn domains "Available domains." [] (vals @+domains+))

(defn clear-domains "Clear add domains from table." [] (swap! +domains+ (fn [_] {})))

(defn find-domain "Find a current domain." [domain-name] 
  (or (get @+domains+ domain-name)
      (uerr "Could not find domain named %s." domain-name)))

(defn index-domain "Index domain in global table." [domain]
  (swap! +domains+ assoc (:name domain) domain))

;;; Establish a dynamically-bound top-level Domain for reasoners.
;;; -------------------------------------------------------------------------

(def ^:dynamic *domain* "Current top-level domain." nil)

(defmacro with-domain "Bind the value of the top-level domain within the extend of this code."
  [domain & body]
  `(let [dom# ~domain] ; Avoid multiple evals for complex domain expressions.
     (binding [*domain* (if (instance? Domain dom#) dom# (find-domain dom#))]
     ~@body)))


;;; Create and index a new Domain record.
;;; -------------------------------------------------------------------------

(defn make-domain 
  "Make a single domain, including subdomains and tests as needed."
  ([name subdomains recognizer-tests doc]
    (let [subs (map find-domain subdomains)
          tests recognizer-tests] ; Rec tests field is mutable.
      (doto (Domain. name subs tests doc)
        index-domain)))
  ([name subdomains recognizer-tests]
    (make-domain name subdomains recognizer-tests ""))
  ([name subdomains]
    (make-domain name subdomains '()))
  ([name]
    (make-domain name '())))

;; This version defines all the tests at once and adds them.
(defmacro defDomain [domain-name subdomains doc-string & recognizer-test-defs]
  `(let [rectests# (reduce (fn [m# rec#] (assoc m# (:name rec#) rec#)) {} (list ~@recognizer-test-defs))]
     (make-domain '~domain-name ~subdomains rectests# ~doc-string)))

