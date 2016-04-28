(ns dray.manager-test
  "Testing routines for Layer and LayerList objects"
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [dray.util :refer [private-function]]
            [dray.corpus :refer [corpus]]
            [dray.wset :refer [ws-descendants]]
            [dray.manager :refer :all]
            [dray.doc :refer [get-vdocument]]
            ))


(defn ex1-file [] (corpus :core-test 0))

(defn ex1-vdoc "Return example VDocument." []
  (get-vdocument (ex1-file)))

(def ex1-vdoc-μ "Return example VDocument. Memoized." (memoize ex1-vdoc))
    
(defn ex1-ws-map "Return example overlay map for ex1-file." [] (edn/read-string (slurp "./resources/test-data/1_Ki_2014_overlay.edn")))

(defn ex1-dm "Return ex1 data manager." [] (make-data-manager (ex1-vdoc-μ)))

(defn sample-pdf-with-overlay 
  "Create a sample vdocument with table overlay. Returns data manager."[]
  (doto (make-data-manager (corpus :core-test 0))
    (dm-restore-ws-from-overlay)))


;;; LAYERS

(deftest make-layer-test 
  (let [l1 (make-layer "Layer1")]
    (testing "Test layer construction and access functions."
       (is (= "Layer1" (.getName l1)))
       (is (empty? (.getRep l1)))
       (is (empty? (.getItems l1)))
       )))

;;; DATA MANAGER FROM VDOC

(deftest data-manager-test
  (let [dm (ex1-dm)
        root-ws (.getHeadWorkingSet dm)
        page-wss (.getChildren root-ws)
        overlay-map (ex1-ws-map)
        page-overlays (:children overlay-map)]
    (testing "Testing the data manager."
      (is (= 4 (count page-wss))) ;; 11 pages in example.
      (is (= 4 (count page-overlays)))
      )))

#_(deftest replace-keys-test
   (let [replace-keys (private-function replace-keys dray.manager)]
     (testing "Test replacing keys using a function."
       (is (= (replace-keys {:foo 1} #(.getName %)) {"foo" 1}))
       (is (= (replace-keys {1 2} inc) {2 2}))
       )))

(deftest stringify-rep-maps-test
  (let [->keyword #(if (string? %) (keyword %) %)
        ->string #(if (keyword? %) (str %) %)
        stringify-rep-maps (private-function stringify-rep-maps dray.manager)]
    (testing "Testing the stringify rep maps."
      (is (= {"foo" 1} (stringify-rep-maps {:foo 1}) ))
      (is (= {} (stringify-rep-maps {}) ))
      (is (= '({"foo" 1}) (stringify-rep-maps '({:foo 1})) ))
      (is (= {"11.0" {"foo" 1}} (stringify-rep-maps {11.0 {:foo 1}})))
      (is (= '({"numgroups" 6, "blobs" ({"id" 0})}) ; Check for tricky lazy sequence bug.
             (stringify-rep-maps (list {:numgroups 6, :blobs (lazy-seq  '({:id 0}))}))))
      )))
     
;;; DATA MANAGER WITH EXISTING OVERLAY
(deftest manager-ws-overlay-test
  (let [dm #_(make-data-manager (corpus :core-test 0))
        (sample-pdf-with-overlay)]
    (testing "Test loaded overlay."
      (let [head-ws (.getHeadWorkingSet dm)
            ws-tables (ws-descendants head-ws :tag "table")
            ws-headers (ws-descendants head-ws :tag "header")]
        (is (= 1 (count ws-tables))) ; One table ws should now exist.
        (is (< 0 (count ws-headers))) ; With multiple header ws.
        ))))


