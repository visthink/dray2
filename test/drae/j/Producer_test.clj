(ns drae.j.Producer-test
  "Testing routines for producers."
  (:import (drae.j.Producer.Table)
           (drae.j.Producer.Entry))
  (:require [clojure.test :refer :all]
            #_[clojure.repl :refer :all]
            #_[clojure.inspector :refer :all]
            [drae.util :refer [private-function]]
            [drae.j.Producer :refer :all]
            ))

(deftest test-producers
  (let [p1 (make-producer :diagram-extract "Diagram extract" "This is a test diagram extract" #( 'my-diagram-extract))]
    (testing "Creation of producers."
              (is (= (:name p1) "Diagram extract"))
             (is (function? (:fn p1)))
             (is (function? (.fn p1)))
             (is (= "diagram-extract" (.key p1)))
             )))

(deftest test-producer-table
  (let [table (atom {}) ;; Test local table only to avoid interfering with global table.
        add-producer (private-function add-producer drae.j.Producer)
        all-producers (private-function all-producers drae.j.Producer)
        get-producer (private-function get-producer drae.j.Producer)
        apply-producer (private-function apply-producer drae.j.Producer)]
    (add-producer :one "One" "Doc string" + table)
    (add-producer :two "Two" "Doc string 2" - table)
    (add-producer :three "Three" "Supercedes previous" * table)
    (testing "Adding producers to a table."
       (is (= (set '("One" "Two" "Three")) 
              (set (map :name (all-producers table)))))
       (is (= + (:fn (get-producer :one table))))
       (is (= - (:fn (get-producer :two table))))
       (is (= -1 (apply-producer :two 1 table)))
    )))

(deftest test-producer-table-java
  (add-ws-producer :one "One" "Doc string" +) ;; Test addition.
  (testing "Retrieve producers from table."
     (is (pos? (count (drae.j.Producer.Table/allWSProducers))))
     (is (= "one" (.key (drae.j.Producer.Table/getWSProducer "one"))))
     (is (= + (.fn (drae.j.Producer.Table/getWSProducer "one"))))
     ))

#_(deftest test-producer-java
   (let [p1 (MyProducer. :one "One" "Doc string" +)]
     (is (instance? Producer p1))
     ))