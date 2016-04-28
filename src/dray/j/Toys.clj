(ns dray.j.Toys
  "A set of testing (toy) routines for system development."
  (:import  (java.util ArrayList)
            (java.lang String))
  (:require [clojure.inspector :refer [inspect-tree inspect-table]]
            [dray.util.inspect :refer [inspect]])
  (:gen-class :name dray.j.Toys
              :state state
              :constructors {[] []} 
              :prefix "-"
              :methods  
              [ ^{:static true} [getWSToyList [] java.util.List] 
                ^{:static true} [callWSToy [java.lang.String java.lang.Object] java.lang.Object]
                ^{:static true} [getSelToyList [] java.util.List]
                ^{:static true} [callSelToy [java.lang.String java.lang.Object] java.lang.Object]
               ]
              ))

(def dray-ws-toys "Map of toy names to toy functions."
  (atom 
    {"New Inspect Working Set (as Table)" 
        (fn [ws] (inspect (.getItems ws) :inspector-type :table :title (format "Working Set %s" ws)))  
     "New Inspect Working Set (as Tree)" 
        (fn [ws] (inspect (.getItems ws) :title (format "Working Set %s" ws)))
     }))

(def dray-sel-toys "Map of selected element toy functions."
  (atom
    {"Inspect Selection (as Table)" #(inspect % :inspector-type :table)
     "Inspect Selection (as Tree)" #(inspect %)
     }))

(defn call-ws-toy 
  "Call a prespecified toy function on the given working set."
  [toy-name ws]
  ((get @dray-ws-toys toy-name) ws)
  )

(defn call-sel-toy 
  "Call a prespecified toy function on the given working set."
  [toy-name els]
  ((get @dray-sel-toys toy-name) els))

(def -callWSToy "Java method for call-ws-toy." call-ws-toy)  

(def -callSelToy "Java method for call-sel-toy." call-sel-toy)

(defn get-WS-toy-list 
  "Returns list of the available Toys."
  []
  (into '() (keys @dray-ws-toys)))

(defn get-sel-toy-list 
  "Returns a list of available toys."
  []
  (into '() (keys @dray-sel-toys)))

(def -getWSToyList "Java method for get-WS-toy-list" get-WS-toy-list) 

(def -getSelToyList "Java method for get-sel-toy-list" get-sel-toy-list)

(defn add-ws-toy [name function]
  (swap! dray-ws-toys assoc name function))

(defn remove-ws-toy [name]
  (swap! dray-ws-toys dissoc name))

(defn add-sel-toy [name function]
  (swap! dray-sel-toys assoc name function))

(defn remove-sel-toy [name function]
  (swap! dray-sel-toys dissoc name))

