(ns drae.wset-test
  "Testing routines for Layer and LayerList objects"
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [drae.corpus :refer [corpus]]
            [drae.manager :refer :all]
            [drae.doc :refer [get-vdocument]]
            [drae.wset :refer [ws-descendants]]
            ))


(defn ex1-file [] (corpus :core-test 0))

(defn ex1-vdoc "Return example VDocument." []
  (get-vdocument (ex1-file)))

(def ex1-vdoc-μ "Return example VDocument. Memoized." (memoize ex1-vdoc))
    
(defn ex1-ws-map "Return example overlay map for ex1-file." [] (edn/read-string (slurp "./resources/test-data/1_Ki_2014_overlay.edn")))

(defn ex1-dm "Return ex1 data manager." [] (make-data-manager (ex1-vdoc-μ)))

;;; WORKING SET OVERLAYS 

(deftest ws-map-read-test
  (let [ws-map (ex1-ws-map)] ;; Just a quick test of the read.
    (testing "Test result of reading in working set overlay map."
      (is (= 'working-set (:type ws-map)))
      (is (= 738.0 (get-in (first (:children ws-map)) [:bbox :height])))
      (is (nil? (:bbox ws-map))) ;; No bbox at root level.
      (is (= 2 (get (second (:children ws-map)) :page)))
  )))

     
;;; DATA MANAGER WITH EXISTING OVERLAY
(deftest manager-ws-overlay-test
  (let [dm (make-data-manager (corpus :core-test 0))]
    (testing "Test loading an overlay."
      (dm-restore-ws-from-overlay dm) ;; Restore the default overlay.
      (let [head-ws (.getHeadWorkingSet dm)
            ws-tables (ws-descendants head-ws :tag "table")
            ws-headers (ws-descendants head-ws :tag "header")]
        (is (= 1 (count ws-tables))) ; One table ws should now exist.
        (is (< 0 (count ws-headers))) ; With multiple header ws.
        ))))
