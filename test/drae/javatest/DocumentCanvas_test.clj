(ns drae.javatest.DocumentCanvas-test
  "*Test the DocumentCanvas class on the Java side.  
    Also contains some general testing routines.*"
  (:import (javax.swing JPanel ImageIcon SwingConstants ListCellRenderer)
           (com.leidos.bmech.model DataManager)
           (com.leidos.bmech.gui DocumentCanvas ThumbView))
  (:require [clojure.test :refer :all]
            [seesaw.core  :refer :all]
            [drae.corpus  :refer :all]
            [drae.doc     :refer :all]
            [drae.newgui  :refer :all]))


(defn dm-painter [oldcanvas widget g2d]
 ; (println "Drawing current page");
 ; (println "Graphics context clip: " (.getClip g2d))
 ; (println "Widget under consideration: " widget)
  (let [dm (.getDataManager oldcanvas)
        scale (.getScale oldcanvas)]
    (.drawPage oldcanvas g2d (.getCurrentPage dm) scale)))

;; Now, can we create a controller for scale and the mouse wheel?
(defn wheel-moved-handler [event]
  (println "Mouse wheel moved")
  (let [rotation (.getWheelRotation event)
        seesaw-canvas (to-widget event)
        old-canvas (user-data seesaw-canvas)
        scale (.getScale old-canvas)
        ]
    (if (< rotation 0) 
      (.setScale old-canvas (* scale 1.05))
      (.setScale old-canvas (* scale 0.95)))
    (.repaint seesaw-canvas)
    ))


(defn make-page-renderer-for [cvdoc2 dm]
  (fn [^ListCellRenderer this {:keys [value index]}]
    (println "Rendering: " value)
    (println "DM is " dm)
    (println "CV is " cvdoc2)
    (println "This is " this)
    (println "Class of this is " (class this) "-> " (ancestors (class this)))
    (let [g2d (.getGraphics this)
          scale (.getScale cvdoc2)]
      (println "Graphics is " g2d)
      (println "Scale is " scale)
      (if-not (nil? g2d)
        (do
         (println "Drawing page")
         (canvas :paint (fn [widget g] (dm-painter cvdoc2 widget g))
                   :user-data cvdoc2
                   :preferred-size [300 :by 400])))
      )
     )
  )
 
#_(defn canvas-popup1 [cv]
   (let [dm (.getDataManager cv)
             page (.getElementAt dm (.getCurrentPage dm))

             filename (-> dm .getPdfFile .getName)
                   
             mycanvas (canvas :paint (fn [widget g2d] (dm-painter cv widget g2d))
                                :user-data cv
                                :preferred-size [600 :by 800]
                                )
             pagelist (listbox :model dm
                                 :user-data cv
                                 :renderer (make-page-renderer-for cv dm))
             frame2 
               (frame :title "NEW!"
                        :content (scrollable pagelist :vscroll :as-needed)
                        :height 800 :width 600)

                      ]
         (listen 
            mycanvas
                  :mouse-wheel-moved wheel-moved-handler
                  )
         (doto frame2 show!)
         ))

(defn canvas-popup [cv]
  (let [dm (.getDataManager cv)
        page (.getElementAt dm (.getCurrentPage dm))

        filename (-> dm .getPdfFile .getName)
                   
        mycanvas (canvas :paint (fn [widget g2d] (dm-painter cv widget g2d))
                         :user-data cv
                         :preferred-size [600 :by 800]
                         )
        frame 
          (frame :title (str "Canvas: " filename)
                 :content  (scrollable mycanvas 
                                       :hscroll :always :vscroll :always
                                       :id :#scrollable :border 10)   
                 )
                 ]
    (listen 
     mycanvas
           :mouse-wheel-moved wheel-moved-handler
           )
    (doto frame pack! show!)
    ))

(defn frametest1 []
  (let [dm (sample-datamanager :core-test 2)
        cv (com.leidos.bmech.gui.DocumentCanvas. dm)
        tv (com.leidos.bmech.gui.ThumbView. dm)]
    (.popup cv)
    (.popup tv)
    dm))

