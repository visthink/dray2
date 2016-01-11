(ns drae.spreadsheet-test
  (:require [clojure.test :refer :all]
            #_[clojure.repl :refer :all]
            #_[clojure.inspector :refer :all]
            [clojure.java.io :refer [file]]
            [incanter.core :refer [view $ col-names]]
            [drae.doc :refer [get-vdocument]]
            [drae.corpus :refer [corpus]]
            [drae.spreadsheet :refer :all]
            ))

(def Sowa "Example Excel file from Sowa et al., 2009" "./resources/spreadsheets/01_Sowa2009.xls")

(defn sowa-dataset [] (spreadsheet-dataset Sowa))

(defn sowa-map [] (spreadsheet-map Sowa))

(defn sowa-matrix [] (spreadsheet-matrix Sowa))

(def core-test-dir (file "./resources/spreadsheets/core-test"))

(defn excel-file? [f] (let [fname (.getName f)] (or (.endsWith fname ".xls") (.endsWith fname ".xlsx"))))

(defn test-files [] (filter excel-file? (file-seq core-test-dir)))

                  
(deftest test-excel-read
  (testing "Test reading the example file."
    #_(is (= 1 1))
    #_(is (thrown-with-msg? Exception #"Cannot find grouping selector key: .+" (grouping-selector :foobarbat)))
    (is (true? (.exists (file Sowa))))
    (let [smap (sowa-map)]
	      (is (true? (map? smap)))
       )))

