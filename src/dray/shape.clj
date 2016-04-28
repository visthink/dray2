(ns dray.shape
  "Primitive visual shape elements and shape descriptions."
  (:import (java.awt Shape)
           (java.awt.geom GeneralPath))
  (:require [seesaw.core :refer [frame canvas border-panel show!]]
            [seesaw.graphics :refer [draw]]
            [dray.xml :refer [prxml-content]]
            [dray.util :refer [uerr]]
            ))

(defn make-general-path "Create a new GeneralPath from the given list of element maps (from .vec file)."
  [element-map & {:keys [style] :or {style nil}}]
  (let [gp (GeneralPath.)]
    (doseq [[k {:keys [x y x1 y1 x2 y2 x3 y3]}] (prxml-content element-map)]
      ;(println "Looking at " k)
      (case k
        :M (-> gp (.moveTo (Double. x) (Double. y)))
        :L (-> gp (.lineTo (Double. x) (Double. y)))
        :C (-> gp (.curveTo (Double. x1) (Double. y1) (Double. x2) (Double. y2) (Double. x3) (Double. y3)))
        :else (uerr "Bad key in make-general-path: %s." k)))
    gp))

(defn test-frame []
  (frame :title "Test Canvas Frame" :width 500 :height 600
         :content 
         (border-panel :id :bp1 :hgap 5 :vgap 5 :border 5
              :center (canvas :id :canvas :background "#FFFFFF" :paint nil))))
