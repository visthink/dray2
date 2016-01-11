(ns drae.util.map-test
  "Test routines for drae.util.map routines."
  (:require [clojure.test :refer :all]
            ;[clojure.repl :refer :all]
            ;[clojure.pprint :refer :all]
            [drae.util.map :refer :all]
          ))

(deftest contain-key-vals-test
  (let [map1 {:foo 1 :bar :ny :bat 3.0}]
   (testing "Key vals containment test"
     (is (contains-key-vals? map1 :foo 1))
     (is (contains-key-vals? map1 :bar :ny))
     (is (not (contains-key-vals? map :foo 23)))
     (is (contains-key-vals? map1 :bat 3.000000001))
     (is (not (contains-key-vals? map1 :bat 3.2)))
     (is (contains-key-vals? map1 :bar :ny :foo 1 :bat 3.00000001)))))

(deftest map-subset-test
  (let [map1 {:foo 1 :bar :ny :bat 3.0}
        map2 {:bar :ny :bat 3.0000001}
        map3 {:bar :ny :bat 3.0 :foo 1}]
    (testing "Map subset tests."
      (is (map-subset? map2 map1))
      (is (not (map-subset? map1 map2)))
      (is (map-subset? map1 map3))
      (is (map-subset? map3 map1))
      (is (not (map-subset? {:foo 42} map1)))
      (is (map-subset? {} map1))
      (is (not (map-subset? map1 {}))))
    (testing "Maps equal tests"
      (is (maps-equal? map1 map3))
      (is (not (maps-equal? map1 map2))))))

(deftest test-remove-null-keys
  (testing "Test remove-null-keys function."
    (is (= {} (remove-null-keys '{:foo nil, :bar nil})))
    (is (= {:this 1} (remove-null-keys '{:foo nil, :this 1})))
    (is (= {:this false, :that true} (remove-null-keys '{:that true, :this false, :other nil})))
    ))

(deftest test-select-keys-if
  (testing "Test select-keys-if method."
    (is (= (select-keys-if {:a 1 :b 2 :c 3 :d 4} (fn [_ v] (odd? v))) ;; Select odd-valued keys. 
           {:a 1 :c 3}))
    (is (= (select-keys-if '{:a :a, :b :c, :d :e} (fn [k v] (= k v))) ;; Select keys that are same as values.
           '{:a :a}))
    ))

(deftest test-merge-map-keys 
  (testing "Test merge-map-keys function."
    (let [m1 '{:a (1 3 5) :b (2 4 6)}]
      (is (= (merge-map-keys m1 [:a :b] :c)
             '{:c (1 3 5 2 4 6)}))
      (is (= (merge-map-keys m1 [:a :b] :c (comp sort concat))
             '{:c (1 2 3 4 5 6)}))
      (is (= (merge-map-keys m1 [:a :b] :c #(sort-by - (concat %1 %2)))
             '{:c (6 5 4 3 2 1)}))
      (is (= (merge-map-keys m1 [:a] :c)
             '{:c (1 3 5) :b (2 4 6)}))
      (is (= (merge-map-keys '{} [:a] :c)
             '{:c nil}))
      )))

(deftest test-apply-fn-map
  (testing "apply fn map"
    (is (maps-equal? (apply-fn-map {:product *, :sum +} 1 2 3 4) {:product 24, :sum 10}))
    (is (maps-equal? (apply-fn-map {:product *, :sum +}) {:product 1, :sum 0})) ; test w/ no args.
    (is (maps-equal? (apply-fn-map {"product" * 'sum +} 1 2 3 4) {"product" 24, 'sum 10}))) ; test with non-keyword keys.
  )