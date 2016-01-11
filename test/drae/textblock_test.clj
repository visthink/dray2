(ns drae.textblock-test
  (:import (drae.j.VisualElement VText))
  (:require [clojure.test :refer :all]
            #_[clojure.zip :as z]
            [drae.corpus :refer [corpus]]
            [drae.util :refer [private-function instances]]
            #_[drae.j.VisualElement :refer [items]]
            [drae.doc :refer [get-vdocument pages p2x-prxml prxml->vel]]
            [drae.textblock :refer :all]
            )
  )

(deftest test-find-textblocks
  (testing "Test the ability to find textblocks."
    (let [v   #_(->> (corpus :core-test 1) p2x-prxml prxml->vel pages first (.getItems) (instances VText))
              (->> (corpus :core-test 1) get-vdocument pages first (.getItems ) (instances VText))
          v1  (first v)
          v2  (second v)
          v5  (nth v 4)
          v6  (nth v 5)
          v50 (take 50 v)
          jpattern (:justified +textblock-patterns+)]
      (is (instance? drae.j.VisualElement.El v1))
      #_(println "V50 is " v50)
      )))

