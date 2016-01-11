(ns drae.ext.ghostscript-test
  "Testing routines for Paxtools utilities."
  (:require [clojure.test :refer :all]
          ;  [clojure.repl :refer :all]
            [clojure.java.shell :refer [sh]]
            [drae.ext.ghostscript :refer :all]
            ))

(deftest ghostscript-tests 
  (if (use-ghostscript?)
    (testing "Ghostscript calls."
       (is (zero? (:exit (sh (ghostscript-path)))));; Call with no params.
       )
    (println "Skipping Ghostscripts - not supported on this machine.")))

