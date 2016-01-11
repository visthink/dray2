(ns drae.config-test
  (:import (java.io File))
  (:require [clojure.test :refer :all]
            [clojure.repl :refer :all]
            [clojure.inspector :refer :all]
            [drae.config :refer :all]
            ))

(deftest test-drae-settings-files
  (testing "Test retrieval of drae settings files."
     (is (not (nil? (drae-setting :python-executable))))
     (is (not (nil? (drae-setting :ghostscript-executable))))
     (is (nil? (drae-setting :foobar)))
     ))

