(ns drae.interpreter-test
  "Testing routines for Layer and LayerList objects"
  (:require [clojure.java.io :refer [file]]
            [clojure.test :refer :all]
            [clojure.edn :as edn]
            [drae.util :refer [private-function]]
            [drae.corpus :refer [corpus]]
         ;   [drae.wset :refer [ws-descendants]]
         ;  [drae.manager :refer :all]
            [drae.doc :refer [get-vdocument]]
            [drae.data :refer :all]
            [drae.interpreter :refer :all]
            [drae.region :refer :all]
            ))


(defn ex1-file [] (corpus :core-test 0))

(defn ex1-vdoc "Return example VDocument." []
  (get-vdocument (ex1-file)))

(def ex1-vdoc-μ "Return example VDocument. Memoized." (memoize ex1-vdoc))
    
(def ex1-overlay-file (file "resources/corpora/core-test/1_Ki_2014.json"))

(defn ex1-overlay [] (json->overlay ex1-overlay-file))

(defn ex1-ws-map "Return example overlay map for ex1-file." [] (edn/read-string (slurp "./resources/test-data/1_Ki_2014_overlay.edn")))

(defn ex1-dm "Return ex1 data manager." [] (make-interpreter (ex1-vdoc-μ)))

#_(defn sample-pdf-with-overlay 
   "Create a sample vdocument with table overlay. Returns data manager."[]
   (doto (make-data-manager (corpus :core-test 0))
     (dm-restore-ws-from-overlay)))


;;; Working sets
(deftest load-overlay-test
  (let [overlay (json->overlay ex1-overlay-file)]
    
    ))

;;; LAYERS

#_(deftest make-layer-test 
   (let [l1 (make-layer "Layer1")]
     (testing "Test layer construction and access functions."
        (is (= "Layer1" (.getName l1)))
        (is (empty? (.getRep l1)))
        (is (empty? (.getItems l1)))
        )))

;;; DATA MANAGER FROM VDOC

#_(deftest data-manager-test
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
   (let [replace-keys (private-function replace-keys drae.manager)]
     (testing "Test replacing keys using a function."
       (is (= (replace-keys {:foo 1} #(.getName %)) {"foo" 1}))
       (is (= (replace-keys {1 2} inc) {2 2}))
       )))

#_(deftest stringify-rep-maps-test
   (let [->keyword #(if (string? %) (keyword %) %)
         ->string #(if (keyword? %) (str %) %)
         stringify-rep-maps (private-function stringify-rep-maps drae.manager)]
     (testing "Testing the stringify rep maps."
       (is (= {"foo" 1} (stringify-rep-maps {:foo 1}) ))
       (is (= {} (stringify-rep-maps {}) ))
       (is (= '({"foo" 1}) (stringify-rep-maps '({:foo 1})) ))
       (is (= {"11.0" {"foo" 1}} (stringify-rep-maps {11.0 {:foo 1}})))
       (is (= '({"numgroups" 6, "blobs" ({"id" 0})}) ; Check for tricky lazy sequence bug.
              (stringify-rep-maps (list {:numgroups 6, :blobs (lazy-seq  '({:id 0}))}))))
       )))
     
;;; DATA MANAGER WITH EXISTING OVERLAY
#_(deftest manager-ws-overlay-test
   (let [dm #_(make-data-manager (corpus :core-test 0))
         (sample-pdf-with-overlay)]
     (testing "Test loaded overlay."
       (let [head-ws (.getHeadWorkingSet dm)
             ws-tables (ws-descendants head-ws :tag "table")
             ws-headers (ws-descendants head-ws :tag "header")]
         (is (= 1 (count ws-tables))) ; One table ws should now exist.
         (is (< 0 (count ws-headers))) ; With multiple header ws.
         ))))


