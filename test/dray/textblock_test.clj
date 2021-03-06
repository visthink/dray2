(ns dray.textblock-test
  (:import (dray.j.VisualElement VText))
  (:require [clojure.test :refer :all]
            #_[clojure.zip :as z]
            [dray.corpus :refer [corpus]]
            [dray.util :refer [private-function instances]]
            #_[dray.j.VisualElement :refer [items]]
            [dray.doc :refer [get-vdocument pages p2x-prxml prxml->vel]]
            [dray.textblock :refer :all]
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
      (is (instance? dray.j.VisualElement.El v1))
      #_(println "V50 is " v50)
      )))

