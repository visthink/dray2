(ns drae.xml-test
  (:require [clojure.test :refer :all]
            [clojure.zip :as z]
            [drae.xml :refer :all]
            [clojure.repl :refer :all]
            [clojure.inspector :refer :all]
            [clojure.java.io :refer [file]]
            [drae.test-data :refer [+prxml-sample-data+]]
            ))


(deftest test-prxml
  (testing "Test of PRXML routines"
    (is (cljxml-node? '{:tag foo}))
    (is (not (cljxml-node? {})))
    (is (not (cljxml-node? 42)))
    (is (= (cljxml->prxml '{:tag :doc :attrs {} :content ("name.txt")})
           '[:doc "name.txt"]))
    (is (= (cljxml->prxml '{:tag :doc :attrs {:a 1 :b 2} :content ()})
           '[:doc {:a 1, :b 2}]))
    (is (= (cljxml->prxml 
             '{:tag :doc :attrs {:a 1 :b 2} 
               :content
               ({:tag :meta :attrs {}
                 :content
                 ({:tag :filename :attrs {}
                   :content ("myfile.txt")})
                 })
               })
           '[:doc {:a 1 :b 2}
             [:meta
              [:filename "myfile.txt"]]]))
    ))
             
   
(deftest test-prxml-2
  (testing "More tests of PRXML routines."
    (let [e1 '[:doc {:a 1 :b 2} "foo"]
          e2 '[:doc "foo"]
          e3 '[:doc {:a 42 :b 43}]
          e4 +prxml-sample-data+]  ; In drae.test-data.
      (is (= (prxml-tag e1) :doc))
      (is (= (prxml-attrs e1) '{:a 1 :b 2}))
      (is (= (prxml-content e1) '("foo")))
      (is (= (prxml-attrs e2) ()))
      (is (= (prxml-content e2) '("foo")))
      (is (= (:a (prxml-attrs e3)) 42))
      (is (= (prxml-content e3) '())) 
      )))

(deftest test-prxml-zipper
  (testing "Testing the zipper creation function for PRXML."
    (let [p1 '[:doc {:a 1 :b 2}
               [:meta {:c 1 :d 2}
                "text1" "text2" "text3"]
               [:main {:e 3 :f 4}
                "text4" "text5"]]
          z1 (prxml-zipper p1)]
      (is (= "text1" (-> z1 z/down z/down z/node)))
      (is (= "text4" (-> z1 z/down z/leftmost z/right z/down z/node)))
      )))
