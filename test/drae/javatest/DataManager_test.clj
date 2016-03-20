(ns drae.javatest.DataManager-test
  "*Test the DataManager and ThumbView classes on the Java side.  
    Also contains some general testing routines.*"
  (:import (javax.swing JPanel ImageIcon SwingConstants ListCellRenderer)
           (com.leidos.bmech.model DataManager)
           (com.leidos.bmech.gui ThumbView))
  (:require [clojure.test :refer :all]
            [seesaw.core  :refer :all]
            [drae.corpus  :refer :all]
            [drae.doc     :refer :all]
            [drae.newgui  :refer :all]))


(defn thumbs-popup [tv]
  (let [pages (-> tv .getDataManager .getPages)
        image-renderer (fn [pageNum] (ImageIcon. (.getThumbnail tv pageNum)))
        imagelist   (listbox :model pages
                             :renderer  (fn [^ListCellRenderer this {:keys [value index]}]
                                          (println "Class of this is " (class this))
                                          (println "Graphics of this is " (.getGraphics this))
                                          (println "   class is " (class (.getGraphics this)))
                                            (println "Rendering: " value)
                                            (doto this
                                              (.setIcon (image-renderer index))
                                              (.setText (str "Page " (inc index)))
                                              (.setHorizontalTextPosition SwingConstants/CENTER)
                                              (.setVerticalTextPosition SwingConstants/BOTTOM))))
        filename (-> tv .getDataManager .getPdfFile .getName)
        frame 
          (frame :title (str "Thumbs: " filename)
                 :content (scrollable imagelist :vscroll :as-needed)
                 :height 500 :width (.getWidth imagelist))]
    (doto frame pack! show!)))

(deftest test-datamanager
  (testing "Test the DataManager constructor."
    (let [dm (sample-datamanager)]
      ;; Object creation?
      (is (instance? drae.j.VisualElement.VDocument (.getVDocument dm)))
      ;; Setting the current page?
      (.setCurrentPage dm 3)
      (is (= 3 (.getCurrentPage dm)))
      )
    )
  )
