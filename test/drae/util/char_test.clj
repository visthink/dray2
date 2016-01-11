(ns drae.util.char-test
  (:require [clojure.test :refer :all]
            #_[clojure.repl :refer :all]
            #_[clojure.pprint :refer :all]
            [drae.util.char :refer :all]
          ))

(deftest char-range-test
  (testing "char-range function"
     (is (= (char-range \a \c) '(\a \b \c)))
     (is (empty? (char-range \a 27)))))
     
(deftest replace-char-with-string-test
  (testing "replace-char-with-string function"
     (is (= "one is the loneliest number"
            ((char-replacer '{\1 "one"}) "1 is the loneliest number")))
     (is (= "two can be as bad as one"
            ((char-replacer '{\2 "two", \1 "one"}) "2 can be as bad as 1")))
     (is (= "alpha-sheet"
            ((char-replacer *greek-map*) "α-sheet")))
     (is (= "beta-catenin"
            ((char-replacer *greek-map*) "β-catenin")))
     ))