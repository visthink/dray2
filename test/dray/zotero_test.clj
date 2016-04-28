(ns dray.zotero-test
  (:import (java.io File))
  (:require [clojure.test :refer :all]
           ; [clojure.repl :refer :all]
           ; [clojure.inspector :refer :all]
            [dray.zotero :refer :all]
            ))

(deftest read-zotero-items-test
  (testing "Testing read of Zotero items from high-throughput library."
     (is (= "Big Mechanism Kinetics" (:name (zlib :bm-kinetics))))
     (let [items (zlib-items (zlib :bm-kinetics))]
       (is (pos? (count items)))
       )))
