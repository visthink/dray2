(ns drae.j.Doc-test
  "Testing routines for visual element creation."
  (:import (drae.j.VisualElement VDocument VPage VText))
  (:require [clojure.test :refer :all]
            ;[clojure.repl :refer :all]
            ;[clojure.inspector :refer :all]
            [drae.j.Doc :refer :all]
            ))

(deftest doc-test
  (testing "Creation of VDocument elements"
    (let [vdoc (VDocument. "filename" [])]
      (is (instance? VDocument vdoc)) ;; Did it create the instance?
      (is (= "filename" (.filename vdoc))
          ))))

