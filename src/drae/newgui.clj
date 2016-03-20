(ns drae.newgui
  "*A set of new GUI routines that shadow those in the Java code.
    Mostly used for development and testing.*"
  (:import (javax.swing JPanel ImageIcon SwingConstants ListCellRenderer)
           (com.leidos.bmech.model DataManager)
           (com.leidos.bmech.gui DocumentCanvas ThumbView))
  (:require [seesaw.core  :refer :all]
            [drae.corpus  :refer :all]
            [drae.doc     :refer :all]))

;; Create Java GUI equivalents 
;;
(defn sample-datamanager 
  "Return a single sample data manager object."
  ([corpus-name corpus-index] 
   (DataManager. (corpus corpus-name corpus-index)))
  ([corpus-index] (sample-datamanager :core-test corpus-index))
  ([] (sample-datamanager 0)))

(defn sample-canvas 
  "Return a single document canvas"
  ([dm] 
    (com.leidos.bmech.gui.DocumentCanvas. dm))
  ([] (sample-canvas (sample-datamanager))))

(defn sample-thumbview
  "Return a single sample thumbview."
  ([data-manager] (com.leidos.bmech.gui.ThumbView. data-manager))
  ([corpus-name item-num]
    (sample-thumbview (sample-datamanager corpus-name item-num)))
  ([]
    (sample-thumbview :core-test 0)))

(defn sample-frame 
 "Create an instance of the Thumb and Canvas views connected
   to the same DataManager."
 ([corpus-name corpus-index]
   (let [dm (sample-datamanager corpus-name corpus-index)
         cv (sample-canvas dm)
         tv (sample-thumbview dm)]
     (.popup cv)
     (.popup tv)
     {:data-manager dm :canvas cv :thumbs tv}))
 ([] (sample-frame :core-test 0)))


;; ThumbView routines

(defn my-thumbview-renderer [^ListCellRenderer this {:keys [value index]}]
  (println "Class of this is " (class this))
  (println "Graphics of this is " (.getGraphics this))
  (println "   class is " (class (.getGraphics this)))
  (println "Rendering: " value)
  (println "Label?: " (.getLabelFor this))
  
  (doto this
 ;   (.setIcon (image-renderer index))
    (.setText (str "Page " (inc index)))
    (.setHorizontalTextPosition SwingConstants/CENTER)
    (.setVerticalTextPosition SwingConstants/BOTTOM))
  )

(defn my-thumbview [dm]
  (listbox :model dm
           :renderer my-thumbview-renderer
       ))

(defn popup-thumbview [tv]
  (let [filename (-> tv .getModel .getPdfFile .getName)
        frame 
        (frame :title (str "MyThumbView: " filename)
               :content (scrollable tv :vscroll :as-needed)
               :height 500 :width 200)]
    (doto frame pack! show!)))



