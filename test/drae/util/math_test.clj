(ns drae.util.math-test
  "Test routines for drae.util.math routines."
  (:require [clojure.test :refer :all]
            [drae.util.math :refer :all]
          ))

(deftest test-=*
   (testing "Approximate = test"
     (is (=* 1 1))
     (is (=* 1.0 1.0))
     (is (=* 1.0 1.000001))
     (is (not (=* 1.0 1.1)))
     ))

(deftest test-min-max
 (testing "Min-max function"
    (is (= [1 5] (min-max '(1 4 5 3 4 2))))
    (is (= [0 0] (min-max '(0))))
    (is (= [-3 -2]  (min-max '(-3 -2 -2.5))))
    ))

(deftest test-abs-diff
 (testing "Abs diff function."
    (is (= 1 (abs-diff 3 4)))
    (is (thrown? AssertionError (abs-diff 3 :bad-arg)))
    (is (thrown? AssertionError (abs-diff :bad-arg 4)))
    ))