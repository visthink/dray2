(ns drae.corpus-test
  (:import (java.io File))
  (:require [clojure.test :refer :all]
           ; [clojure.repl :refer :all]
           ; [clojure.inspector :refer :all]
            [drae.corpus :refer :all]
            ))

(deftest test-corpus-function
  (testing "Test the corpus function."
     (is (instance? File (corpus :core-test 0))) ; Simple test - returns a file?
     (is (pos? (count (corpus :core-test))))     ; Returns multiple files.
     (is (thrown-with-msg? Exception #"Corpus .+ has items from .+. There is no item at index .+" (corpus :core-test 100000)))
     (is (thrown-with-msg? Exception #"Cannot find corpus named .+ in DRAE configuration files." (corpus :nonexistant-corpus 0)))
     ))