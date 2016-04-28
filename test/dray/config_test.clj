(ns dray.config-test
  (:import (java.io File))
  (:require [clojure.test :refer :all]
            [clojure.repl :refer :all]
            [clojure.inspector :refer :all]
            [dray.config :refer :all]
            ))

(deftest test-dray-settings-files
  (testing "Test retrieval of dray settings files."
     (is (not (nil? (dray-setting :python-executable))))
     (is (not (nil? (dray-setting :ghostscript-executable))))
     (is (nil? (dray-setting :foobar)))
     ))

