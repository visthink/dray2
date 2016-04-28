(ns dray.doc-test
  (:import (dray.j.VisualElement VText)
           (java.io File))
  (:require [clojure.test :refer :all]
            [clojure.zip :as z]
            [dray.corpus :refer [corpus]]
            [dray.util :refer [private-function instances]]
            [dray.j.VisualElement :refer [items]]
            [dray.doc :refer :all]
            )
  )

(deftest test-bboxes
  (testing "Creation of bounding boxes."
    (is (= 10.0 (.getWidth (make-bbox 0.0 0.0 10.0 10.0)))) ; Test regular use.
    (is (= 11.0 (.getHeight (make-bbox 0 0 10 11)))) ; Test integer args.
    (is (= 12.0 (.getX (make-bbox "12.0" "13.0" "14.0" "15.0")))) ; Test strings.
    (is (= 10.0 (.getWidth (make-bbox [0.0 0.0 10.0 10.0])))) ; Test list argument.
    ))

(deftest test-string->boolean
  (testing "Test the string->boolean routine."
    (let [string->boolean (private-function string->boolean dray.doc)]
      (is (= true (string->boolean true)))
      (is (= false (string->boolean false)))
      (is (nil? (string->boolean nil)))
      (is (= true (string->boolean "yes")))
      (is (= false (string->boolean "no")))
      (is (= true (string->boolean "true")))
      (is (= false (string->boolean "false")))
      (is (= true (string->boolean "t")))
      (is (= false (string->boolean "F"))) 
      (is (nil? (string->boolean "unknown")))
    )))

(deftest test-text-styles
  (testing "Test the creation of text styles."
    (let [x (make-text-style "Arial" "10.0" "yes" "no" 0.0)] ; note strings for size, bold, ital
      (is (= 10.0 (.font-size x)))
      (is (= 10.0 (:font-size x))) ;; Should also work like a map.
      (is (= true (.bold x)))
      (is (= false (.italic x)))
      (let [y (common-text-style (list x x))]
        (is (= 10.0 (.font-size  y)))
        (is (= true (.bold y)))
      ))))

#_(deftest test-text-styles-with-nil-values
   (testing "Test the system when the text styles include null values."
      (let [x (make-text-style nil nil nil nil nil)]
            )))

(deftest test-common-aspect
  (testing "Test the common-aspect function."
      (let [common-aspect (private-function common-aspect dray.doc)]
        (is (= 1 (common-aspect [1 1 1 1])))
        (is (= nil (common-aspect [1 1 2 3])))
        (is (= 3 (common-aspect [3.4 3.3 3.2 3.15 3] int)))
        (is (= nil (common-aspect '())))
        (is (= 1.5 (common-aspect '(1.5)))
        ))))

(deftest test-vimage
  (testing "Test creation of VImage instances. NOTE: If failing on instance? test, try cleaning and reloading."
    (let [x (make-vimage "image-path/image.jpg" (make-bbox 0.0 0.0 10.0 10.0))
          doc (make-vdocument "/home/user/doc.pdf" (list x))]
      (is (= 10.0 (.getHeight (.bbox x))))
      (is (true? (instance? dray.j.VisualElement.VImage x)))
      (is (instance? File (full-bitmap-path-for x doc)))    
      )))

(def +ex-token1+ 
  '[:TOKEN {:y "544.369", :bold "no", :font-color "#000000", :rotation "0", 
            :font-name "advslimback", :font-size "8.41684", :width "29.262", :angle "0", 
            :id "p1_w40", :x "234.01", :base "550.85", :italic "no", :sid "p1_s44", 
            :height "8.49259"} "multiple"])

(def +ex-text+
  '[:TEXT {:width "223.836", :height "7.64421", 
           :x "69.9857", :y "347.164"} 
    [:TOKEN {:y "347.241", :bold "no", :font-color "#000000", :font-name "AdvSlimbach", 
             :font-size "7.65186", :width "22.4391", :x "69.9857", :italic "no", 
             :sid "p1_s803", :height "7.64421"} "Figure"] 
    [:TOKEN {:y "347.241", :bold "no", :font-color "#000000", :rotation "0", 
             :font-name "advslimbach", :font-size "7.65186", :width "4.13966", 
             :x "94.9667", :base "353.056", :italic "no", :sid "p1_s804", :height "7.64421"} "8"] 
    [:TOKEN {:y "347.164", :bold "yes", :font-color "#000000", :font-name "advslimbach", 
             :font-size "7.65186", :width "33.9284", :angle "0", :id "p1_w738", 
             :x "102.915", :base "353.056", :italic "no", :sid "p1_s805", 
             :height "7.72073"} "Schematic"]])
    
(deftest token-test
  (testing "Create a token from PRXML"
    (let [token (prxml->vel +ex-token1+)]
      (is (= 544.369 (.getY (.getBbox token))))
      (is (= "advslimback" (.font-name (.style token))))
      (is (= false (.bold (.style token))))
      )
    ))

(deftest text-test
  (testing "Create a VText from PRXML."
     (let [vtext (prxml->vel +ex-text+)]
       (is (= 223.836 (.width (.bbox vtext))))
       (is (= "Figure 8 Schematic" (.text vtext)))
       (is (nil? (.bold (.style vtext)))) ;; Not all the same.
       (is (= false (.italic (.style vtext))))     
       )))

(deftest test-next-up
  (testing "Test the next-up function."
    (let [next-up (private-function next-up dray.doc)
          atree '[[a b] [[c] [c [e f] g [h i]]] j]
          z1 (z/vector-zip atree)]
      (is (= atree (z/node z1)))
      (is (nil? (next-up z1))) ; Root (should return nil)
      (is (= 'j (-> z1 z/down next-up next-up z/node)))  ; Sibling condition.
      (is (nil? (-> z1 z/down next-up next-up next-up))) ; Last leaf condition.
      (is (= 'g (-> z1 z/down z/right z/down z/right z/down z/right z/down z/right next-up z/node)))
      )))

(deftest test-get-vdocument
  (testing "Testing get-vdocument on :core-test 1 pdf."
    (let [v (get-vdocument (corpus :core-test 0))
          page? (partial instance? dray.j.VisualElement.VPage)
          vel? (partial instance? dray.j.VisualElement.El)
          vtext? (partial instance? dray.j.VisualElement.VText)
          vdoc? (partial instance? dray.j.VisualElement.VDocument)]
      (is (= 4 (count (.getItems v))))
      (is (= 4 (count (pages v)))) ; 4 items in document...
      (is (= 68 (-> v pages first items count)))
      (is (every? page? (.getItems v))) ; ..which are pages.
      (let [p1-elements (.getItems (first (.getItems v)))]                  ; All Page 1 elements..
        (is (every? vel? p1-elements))    ; ..are visual elements..
        (is (some vtext? p1-elements))   ; ..and some are texts.
       )
      (is (vdoc? (get-vdocument (corpus :core-test 2)))) ; Test filename with space in name.
      )))

(deftest test-merge-adjacent-texts
  (testing "Test the merge-adjacent-tests functions."
    (let [v   (->> (corpus :core-test 1) p2x-prxml prxml->vel pages first (.getItems ) (instances VText))
          v1  (first v)
          v2  (second v)
          v5  (nth v 4)
          v6  (nth v 5)
          v50 (take 50 v)]
      (is (= "2_Mor_2011.pdf" (.getName (corpus :core-test 1)))) ;; Check the filename.
      (is (false? (texts-adjacent? v1 v2)))
      (is (true? (texts-adjacent? v5 v6)))
      (let [merged-text (merge-vtexts v5 v6)]
        (is (true? (= (vel-x merged-text) (vel-x v5)))) ; == left-hand sides.
        (is (true? (= (vel-max-x merged-text) (vel-max-x v6)))) ; == right-hand sides.
        (is (= (count (merge-adjacent-vtexts (list v5 v6))) 1)) ; Are v5, v6 merged into one text?
      
        ))))

