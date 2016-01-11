(ns drae.j.VisualElement-test
  "Testing routines for visual element creation."
  (:import (drae.j.VisualElement VDocument VPage VPage VText VImage)
           (drae.j BoundingBox))
  (:require [clojure.test :refer :all]
            [drae.j.VisualElement :refer :all]
            [drae.doc :refer [make-bbox]]
            ))


(deftest test-replace-item
  (testing "Test replacing a single item in a list with multiple replacements."
     (is (= '(1 1 9 9 9 3) (replace-item-with '(1 1 2 3) 2 '(9 9 9))))
     (is (= '(1 1 2 3) (replace-item-with '(1 1 2 3) -1 '(9 9 9))))
     (is (= '(1 1 2 9 9 9) (replace-item-with '(1 1 2 3) 3 '(9 9 9))))
  ))

(def bbox (BoundingBox. 0.0 0.0 10.0 10.0))

(def vp1 (VPage. 1 bbox '[]))

(deftest test-vPage
  (testing "Tests for the new VPage object (defined via deftype)."
    (let [bbox (BoundingBox. 0.0 0.0 10.0 10.0)
          vp1 (VPage. 1 bbox '[])]
      (is (= (class bbox) BoundingBox))
      (is (= (class vp1) VPage))
      (is (= (.number vp1) 1))
      (is (= (.height (.bbox vp1)) 10.0))
      (is (empty? (.getItems vp1)))
      (do (.setItems vp1 '(1 2 3))
        (is (= (.getItems vp1) '(1 2 3))))
      
      )
    ))


