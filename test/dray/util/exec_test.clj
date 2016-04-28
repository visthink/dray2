(ns dray.util.exec-test
  (:require [clojure.test :refer :all]
            [dray.util.exec :refer :all]
          ))

(deftest test-home-dir
  (testing "Test routine to find user's home directory."
     (is (.isDirectory (home-dir)))
  ))

(deftest add-to-system-path-test
  (testing "Test routine for adding to system path"
     (is (string? (add-to-system-path "/usr/local/bin"))) ; weak test - do we get string back?
     ))

