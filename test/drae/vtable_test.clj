(ns drae.vtable-test
  (:import (java.io File)
           (com.leidos.bmech.model DataManager))
  (:require [clojure.test :refer :all]
            #_[clojure.repl :refer :all]
            #_[clojure.inspector :refer :all]
            [clojure.java.io :refer [file]]
            #_[clojure.walk :refer [walk prewalk prewalk-demo]]
            [drae.vtable :refer :all]
            [drae.wset :refer [ws-descendants]]
            [drae.manager-test :refer [sample-pdf-with-overlay]]
         )
  )

    
(deftest test-columns
  (testing "Test columns and tables."
     (is (= 1 1))
     
     ))

(defn sample-extracted-table []
  (first (extract-vtables (sample-pdf-with-overlay))))

(defn sample-table-map []
  (simplify-table (sample-extracted-table)))

(defn sample-table-wsets []
  (ws-descendants (.getHeadWorkingSet (sample-pdf-with-overlay))
                  :tag "table"))