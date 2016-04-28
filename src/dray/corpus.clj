(ns dray.corpus
  "*Routines to access predefined corpora documents. Corpora are defined in the 
    config files `dray-settings.edn` and `dray-default-settings.edn`.*"
  (:import (java.io File))
  (:require [clojure.java.io :refer [file as-url input-stream output-stream copy]]
            [clojure.pprint :refer [print-table]]
            [dray.util :refer [file-type? uerr]]
            [dray.config :refer [dray-setting]]
            )
  )

(defn- pdf-files-at-path [path] 
  (filter #(file-type? % ".pdf") (file-seq (file path))))

(defn corpora-map 
  "Return a map-based description of available corpora."
  [] (dray-setting :corpora))
  
(defn corpus 
  "Returns a list of the files in the corpus with the 
   given keyword-based name. `(corpus)`  or `(corpus :list)` returns a list of
   available corpora. `(corpus name)` returns a list of
   all filenames in that corpus. `(corpus name n)` returns
   just the nth filename (zero-indexed)."
  ([corpus-name]
    (let [cmap (corpora-map)]
      (cond 
        (= corpus-name :keys) (keys cmap)
        (= corpus-name :list) 
          (print-table [:keyword :name :doc]
            (map (fn [[k v]] (assoc v :keyword k)) 
                 (sort-by first cmap)))
        :else 
         (if-let [corpus-desc (get cmap corpus-name)]
           (pdf-files-at-path (:path corpus-desc))
           (uerr "Cannot find corpus named %s in DRAY configuration files." corpus-name))
        )))
  ([corpus-name n] 
    (let [corpus-files (corpus corpus-name)]
      (if (or (neg? n) (>= n (count corpus-files)))
        (uerr "Corpus %s has items from 0-%d. There is no item at index %d." corpus-name (dec (count (corpus corpus-name))) n)
        (nth corpus-files n))))
  ([] (corpus :list)))

#_(defn pubmed-fulltext-url [pubmed-id]
   (as-url (str "http://www.ncbi.nlm.nih.gov/pmc/articles/" pubmed-id "/pdf")))

#_(defn download-to-file [url file]
   (with-open [webdoc (input-stream url), save-file (output-stream file)] (copy webdoc save-file)))

#_(defn download-pubmed-pdf [pubmed-id]
   (download-to-file (pubmed-fulltext-url pubmed-id) (file "/Users/fergusonrw/Downloads/" (str pubmed-id ".pdf"))))


#_(defn test1 []
   (let [u (clojure.java.io/as-url "http://www.nature.com/srep/2014/141106/srep06881/pdf/srep06881.pdf")]
     (download-to-file u (file "/Users/fergusonrw/Downloads/srep06881.pdf"))))
