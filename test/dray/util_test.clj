;;;; Tests for the utiity code.
;;;;
;;;; Ron Ferguson
;;;; Created: 2014-04-22
;;;; Updated: 2014-04-30
;;;;
(ns dray.util-test
  (:import (java.io File)
           (java.nio.file Path)
    )
  (:require [clojure.test :refer :all]
            [dray.util :refer :all]
            [dray.util.map :refer [contains-key-vals?]]
            [clojure.repl :refer :all]
            [clojure.inspector :refer :all]
            [clojure.java.io :refer [file]]
            #_[clojure.data.xml :refer [Element]]
            [dray.test-data :refer [+prxml-sample-data+]]
            [dray.corpus :refer [corpus]]
            ))

(defn const? [x] (:const (meta x)))

(deftest defconst-tests
  (let [[n1 n2 n3] (repeatedly gensym) ; Gen const names and values.
        [v1 v2 v3] (repeatedly #(rand 100))]
    (testing "Single constant definition test"
      (is (const? (eval `(defconst ~n1 ~v1))))
      (is (eval `(= ~n1 ~v1)))
      )
    (testing "Multiple constants definition test"
      (eval `(defconst ~n2 ~v2 ~n3 ~v3))
      (is (const? (eval `(var ,~n2))))
      (is (const? (eval `(var ,~n3))))
      (is (eval `(= ~n2 ~v2)))
      (is (eval `(= ~n3 ~v3)))
    )))


(deftest map-selector-fn-test
  (testing "Map selector fn test"
     (let [ff1 (file "./foo/bar.bat")
           ff2 (file "test.txt")
           sel-fn (map-selector-fn :name getName :parent getParent :dir? isDirectory)]
       (is (contains-key-vals? (sel-fn ff1)
               :name "bar.bat" :parent "./foo" :dir? false))
       (is (contains-key-vals? (sel-fn ff2)
               :name "test.txt" :parent nil :dir? false))
       )))

(deftest hyphenate-classname-test
  (testing "Hyphenate classnames"
     (is (= "text-fragment" (hyphenate-camelcase "TextFragment")))
     (is (= "text-box" (hyphenate-camelcase "TextBox")))
     (is (= "long" (hyphenate-classname (class 1))))
     (is (= "persistent-array-map" (hyphenate-classname (class {}))))
     ))

(deftest test-keyword
  (testing "Test de-keywording"
    (is (= 'foo (de-keyword :foo)))
    (is (= 'foo (de-keyword 'foo)))
    ))

(deftest test-find-classname
  (testing "Test of full-classname-for-symbol"
     (is (= "java.lang.Integer" (full-classname-for-symbol 'Integer)))
     (is (= "clojure.data.xml.Element" (full-classname-for-symbol 'Element 'clojure.data.xml)))
     (is (thrown-with-msg? Exception #"Could not find full classname for symbol .+" (full-classname-for-symbol 'not-a-known-class)))
     ))

(deftest test-cache-dir
  (testing "Test the cache-directory functions."
    (let [pdf (corpus :core-test 0)
          cdir (cache-directory pdf)
          p1 (file "test/myfile.txt")
          p2 (file ".dray-cache/test/myfile.txt")
          p3 (file "resource/corpus/test-core/.dray-cache/test/myfile.txt")
          p4 (file (.getCanonicalPath p3))
          good-result (file cdir "test" "myfile.txt")
          ]
      (is (= good-result (resolve-cache-file-for pdf p1))) ; Test straight add.
      (is (= good-result (resolve-cache-file-for pdf p2))) ; Test one-level overlap.
      (is (= good-result (resolve-cache-file-for pdf p3))) ; Test higher amount of overlap.
     ; (is (= (file (.getCanonicalPath good-result)) (resolve-cache-file-for pdf p4)))
           )))

(deftest test-uerr-and-uerror
  (testing "Testing the uerr and uerror macros."
     (is (thrown-with-msg? Exception #"Foobar!" (uerror "Foobar!")))
     (is (thrown-with-msg? Exception #"Foobar!" (uerr "Foobar!")))
     (is (thrown-with-msg? ArithmeticException #"Div 4 by zero." (uerr ArithmeticException "Div %d by zero." 4)))
     ))
  
(deftest test-filetype?
  (testing "Test the file-type? predicate."
     (let [f (file "myfile.txt")]
       (is (true? (file-type? f ".txt")))
       (is (false? (file-type? f ".pdf")))
       (is (false? (file-type? f ".txttxt")))
       )))

(deftest test-every-other
  (testing "Test every other utility function."
     (is (= (every-other (range 10)) '(0 2 4 6 8)))
     (is (= (every-other (range 11)) '(0 2 4 6 8 10)))
     (is (= (every-other '()) '()))
     (is (= (every-other '(1)) '(1)))
     ))

(deftest test-pairwise-group-by
  (testing "Test the pairwise-group-by function."
    (let [s1 '(1 2 3 6 7 9 10 11 15 18 20 21 22) ; typical.
          s2 '(1 3 5 7) ; none
          s3 '(1 2 3 4) ; all
          s4 '(0 3 5 6 7 9 10 13) ; starting with non-group.
          plus-one? (fn [a b] (= 1 (- b a)))]
      (is (= (pairwise-group-by s1 plus-one?)
             '((1 2 3) (6 7) (9 10 11) (20 21 22))))
      (is (= (pairwise-group-by s2 plus-one?)
             '()))
      (is (= (pairwise-group-by s3 plus-one?)
             '((1 2 3 4))))
      (is (= (pairwise-group-by s4 plus-one?)
             '((5 6 7) (9 10))))
      )))

(deftest test-pairwise-replace
  (testing "Test the pairwise-replace function."
    (let [s1 '(1 2 2 3 4 5 5 10 11) 
          s2 '(1 1 2 3 4 5 1 1) ; merge at start.
          s3 '(1 2 3 3 6)       ; merge at end.
          ]
      (is (= (pairwise-replace s1 = +)
             '(1 4 3 4 20 11)))
      (is (= (pairwise-replace s2 = +)
             '(4 3 4 5 2)))
      (is (= (pairwise-replace s3 = +)
             '(1 2 12)))
      (is (= (pairwise-replace '(1 1) = +)
             '(2)))
      (is (= (pairwise-replace '(1) = +)
             '(1)))
      (is (= (pairwise-replace '() = +)
             '()))
      )))

(deftest test-decode-url
  (testing "Test the decode-url function."
     (is (= (decode-url "p2x-3_Ogura%20et%20al%202009.xml_data/image-2.vec")
            "p2x-3_Ogura et al 2009.xml_data/image-2.vec"))
     (is (= (decode-url "http%3A%2F%2Ffoo%20bar%2F") ;; from rosettacode.org.
            "http://foo bar/"))))


                                                    
     
