(ns dray.stats-test
  (:require [clojure.test :refer :all]
            [dray.doc :refer [get-vdocument pages]]
            [dray.j.VisualElement :refer [items]]
            [dray.corpus :refer [corpus]]
            [dray.stats :refer :all]
            [dray.util :refer [private-function]]
            ))

;; Functions for getting test data from core-test corpus.

(def ex1-vdoc (memoize #(get-vdocument (corpus :core-test 0))))

(def ex1-vels (memoize #(-> (ex1-vdoc) pages first items)))

(def ex2-vels "Vels from Page 3 of first vdoc."
  #(-> (ex1-vdoc) pages (nth 2) items))
  
(deftest test-grouping-selectors
  (testing "Test the table of grouping selectors."
    (let [grouping-selector (private-function grouping-selector dray.stats)]
      (is (true? (function? (grouping-selector :lhs))))
      (is (thrown-with-msg? Exception #"Cannot find grouping selector key: .+" (grouping-selector :foobarbat)))
      )))

(deftest grouping-test
  (testing "Test grouping routines for text elements."
     (let [vels (ex1-vels)
           g1 (groupings :lhs vels)]
       (is (map? g1))
       (is (= 21 (count (:items (get g1 56.7))))) ;; Should be 21 texts with this LHS value.
       (for [selector grouping-selectors]
         (is (map? (groupings selector vels)))) ;; Just test execution.
       )))

(deftest round-tenths-test
  (let [round-tenths (private-function round-tenths dray.stats)] ; Retrieve fn, since private.
    (testing "Test the round-tenths function."
        (is (= (round-tenths 9) 9.0))
        (is (= (round-tenths 9.21) 9.2))
        (is (= (round-tenths 9.99) 10.0))
        (is (nil? (round-tenths "9.99"))) ;; Don't do strings.
        (is (nil? (round-tenths nil))) ;; Nil (no error) for nil, etc.
        (is (nil? (round-tenths 'foo)))
        )))

(deftest stat-summary-test
  (testing "Test the steps of creating a stats summary table."
     (let [vels (texts-in (ex1-vels))
           summary (text-summary-table vels :prune? true)]
       (is (map? summary))
       (is (= 7 (count summary)))
       (is (= '(9.5 9.0 8.0) (keys (:font-size summary))))
       (is (= '(56.7 306.1 96.4) (keys (:lhs summary))))         
     )))

(deftest find-near-misses-test
  (testing "Test the ability to find near misses in the grouping tables."
     (let [vels (texts-in (ex2-vels))
           rhs-map (groupings :rhs (-> (ex2-vels) texts-in))
           near-miss-keys (private-function near-miss-keys dray.stats)]
       (is (= '() (near-miss-keys rhs-map)))
       )))
      
   