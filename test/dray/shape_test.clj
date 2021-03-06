(ns dray.shape-test
  (:require [clojure.test :refer :all]
            #_[clojure.repl :refer :all]
            #_[clojure.inspector :refer :all]
            [clojure.java.io :refer [file]]
            #_[incanter.core :refer [view $ col-names]]
            [dray.corpus :refer [corpus]]
            [dray.shape :refer :all]
            ))

(def ex1 
  ' [:GROUP
     {:sid "p1_s734", :style
      "stroke: #221E1F;fill:none;stroke-width: 1.67223;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.67223;",
      :clipZone "p1_c5"}
     [:M {:x "134.419", :y "116.236"}]
     [:L {:x "269.281", :y "116.236"}]
     [:C
      {:x1 "275.471", :y1 "116.236",
       :x2 "280.49",  :y2 "121.167",
       :x3 "280.49",  :y3 "127.251"}]
  [:L {:x "280.49", :y "329.088"}]
  [:C
   {:x1 "280.49",   :y1 "335.17",
    :x2 "275.471",  :y2 "340.101",
    :x3 "269.281",  :y3 "340.101"}]
  [:L {:x "94.4004", :y "340.101"}]
  [:C
   {:x1 "88.209",   :y1 "340.101",
    :x2 "83.1907",  :y2 "335.17",
    :x3 "83.1907",  :y3 "329.088"}]
  [:L {:x "83.1907", :y "127.251"}]
  [:C
   {:x1 "83.1907",  :y1 "121.167",
    :x2 "88.209",   :y2 "116.236",
    :x3 "94.4004",  :y3 "116.236"}]
  [:L {:x "130.278", :y "116.236"}]]
  )


(deftest initial-shape-parse-test
  (testing "Can we read the initial elements into a General Path object?"
     (let [[tag attrs & entries] ex1
           gp (make-general-path ex1)]
       (is (= tag :GROUP))
           
           )))

(def data1 
 [:VECTORIALIMAGES
  [:CLIP
   {:sid "p1_s1",
    :x "0",
    :y "0",
    :width "612",
    :height "792",
    :idClipZone "p1_c1"}
   [:GROUP
    {:sid "p1_s2", :closed "true"}
    [:M {:x "0", :y "792"}]
    [:L {:x "612", :y "792"}]
    [:L {:x "612", :y "0"}]
    [:L {:x "0", :y "0"}]
    [:L {:x "0", :y "792"}]]]
  [:CLIP
   {:sid "p1_s3",
    :x "21",
    :y "15",
    :width "570",
    :height "762",
    :idClipZone "p1_c2"}
   [:GROUP
    {:sid "p1_s4", :closed "true"}
    [:M {:x "21", :y "777"}]
    [:L {:x "591", :y "777"}]
    [:L {:x "591", :y "15"}]
    [:L {:x "21", :y "15"}]
    [:L {:x "21", :y "777"}]]]
 [:CLIP
  {:sid "p1_s706",
   :x "88.135",
   :y "96.2249",
   :width "187.211",
   :height "241.242",
   :idClipZone "p1_c3"}
  [:GROUP
   {:sid "p1_s707", :closed "true"}
   [:M {:x "88.135", :y "96.2249"}]
   [:L {:x "275.346", :y "96.2249"}]
   [:L {:x "275.346", :y "337.467"}]
   [:L {:x "88.135", :y "337.467"}]
   [:L {:x "88.135", :y "96.2249"}]]]
 [:CLIP
  {:sid "p1_s708",
   :x "88.135",
   :y "96.2249",
   :width "187.211",
   :height "241.242",
   :idClipZone "p1_c4"}
  [:GROUP
   {:sid "p1_s709", :closed "true"}
   [:M {:x "21", :y "777"}]
   [:L {:x "591", :y "777"}]
   [:L {:x "591", :y "15"}]
   [:L {:x "21", :y "15"}]
   [:L {:x "21", :y "777"}]]]
 [:CLIP
  {:sid "p1_s711",
   :x "21",
   :y "15",
   :width "570",
   :height "762",
   :idClipZone "p1_c5"}
  [:GROUP
   {:sid "p1_s712", :closed "true"}
   [:M {:x "21", :y "777"}]
   [:L {:x "591", :y "777"}]
   [:L {:x "591", :y "15"}]
   [:L {:x "21", :y "15"}]
   [:L {:x "21", :y "777"}]]]
 [:GROUP
  {:sid "p1_s713",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "155.1", :y "268.926"}]
  [:L {:x "155.1", :y "262.58"}]
  [:L {:x "165.121", :y "262.58"}]]
 [:GROUP
  {:sid "p1_s714",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "164.668", :y "264.133"}]
  [:L {:x "167.349", :y "262.585"}]
  [:L {:x "164.668", :y "261.036"}]]
 [:GROUP
  {:sid "p1_s717",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "231.61", :y "268.926"}]
  [:L {:x "231.61", :y "262.58"}]
  [:L {:x "241.632", :y "262.58"}]]
 [:GROUP
  {:sid "p1_s718",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "241.178", :y "264.133"}]
  [:L {:x "243.861", :y "262.585"}]
  [:L {:x "241.178", :y "261.036"}]]
 [:GROUP
  {:sid "p1_s721",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.67223;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.67223;",
   :clipZone "p1_c5"}
  [:M {:x "116.523", :y "272.208"}]
  [:C
   {:x1 "118.293",
    :y1 "274.708",
    :x2 "119.159",
    :y2 "279.041",
    :x3 "122.163",
    :y3 "279.041"}]
  [:C
   {:x1 "126.385",
    :y1 "279.041",
    :x2 "126.385",
    :y2 "270.481",
    :x3 "130.604",
    :y3 "270.481"}]
  [:C
   {:x1 "134.826",
    :y1 "270.481",
    :x2 "134.826",
    :y2 "279.041",
    :x3 "139.047",
    :y3 "279.041"}]
  [:C
   {:x1 "143.268",
    :y1 "279.041",
    :x2 "143.268",
    :y2 "270.481",
    :x3 "147.488",
    :y3 "270.481"}]
  [:C
   {:x1 "151.711",
    :y1 "270.481",
    :x2 "151.711",
    :y2 "279.041",
    :x3 "155.934",
    :y3 "279.041"}]
  [:C
   {:x1 "160.155",
    :y1 "279.041",
    :x2 "160.155",
    :y2 "270.481",
    :x3 "164.375",
    :y3 "270.481"}]
  [:C
   {:x1 "168.601",
    :y1 "270.481",
    :x2 "168.601",
    :y2 "279.041",
    :x3 "172.825",
    :y3 "279.041"}]]
 [:GROUP
  {:sid "p1_s722",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.67223;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.67223;",
   :clipZone "p1_c5"}
  [:M {:x "115.763", :y "279.041"}]
  [:C
   {:x1 "119.985",
    :y1 "279.041",
    :x2 "119.985",
    :y2 "270.481",
    :x3 "124.206",
    :y3 "270.481"}]
  [:C
   {:x1 "128.429",
    :y1 "270.481",
    :x2 "128.429",
    :y2 "279.041",
    :x3 "132.646",
    :y3 "279.041"}]
  [:C
   {:x1 "136.868",
    :y1 "279.041",
    :x2 "136.868",
    :y2 "270.481",
    :x3 "141.089",
    :y3 "270.481"}]
  [:C
   {:x1 "145.312",
    :y1 "270.481",
    :x2 "145.312",
    :y2 "279.041",
    :x3 "149.534",
    :y3 "279.041"}]
  [:C
   {:x1 "153.755",
    :y1 "279.041",
    :x2 "153.755",
    :y2 "270.481",
    :x3 "157.977",
    :y3 "270.481"}]
  [:C
   {:x1 "162.202",
    :y1 "270.481",
    :x2 "162.202",
    :y2 "279.041",
    :x3 "166.424",
    :y3 "279.041"}]
  [:C
   {:x1 "170.651",
    :y1 "279.041",
    :x2 "170.651",
    :y2 "270.481",
    :x3 "174.874",
    :y3 "270.481"}]]
 [:GROUP
  {:sid "p1_s723",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.67223;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.67223;",
   :clipZone "p1_c5"}
  [:M {:x "192.392", :y "270.193"}]
  [:C
   {:x1 "196.612",
    :y1 "270.193",
    :x2 "196.612",
    :y2 "278.753",
    :x3 "200.834",
    :y3 "278.753"}]
  [:C
   {:x1 "205.056",
    :y1 "278.753",
    :x2 "205.056",
    :y2 "270.193",
    :x3 "209.275",
    :y3 "270.193"}]
  [:C
   {:x1 "213.498",
    :y1 "270.193",
    :x2 "213.498",
    :y2 "278.753",
    :x3 "217.718",
    :y3 "278.753"}]
  [:C
   {:x1 "221.939",
    :y1 "278.753",
    :x2 "221.939",
    :y2 "270.193",
    :x3 "226.159",
    :y3 "270.193"}]
  [:C
   {:x1 "230.381",
    :y1 "270.193",
    :x2 "230.381",
    :y2 "278.753",
    :x3 "234.606",
    :y3 "278.753"}]
  [:C
   {:x1 "238.826",
    :y1 "278.753",
    :x2 "238.826",
    :y2 "270.193",
    :x3 "243.048",
    :y3 "270.193"}]
  [:C
   {:x1 "247.273",
    :y1 "270.193",
    :x2 "247.273",
    :y2 "278.753",
    :x3 "251.496",
    :y3 "278.753"}]]
 [:GROUP
  {:sid "p1_s724",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.67223;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.67223;",
   :clipZone "p1_c5"}
  [:M {:x "194.432", :y "278.753"}]
  [:C
   {:x1 "198.656",
    :y1 "278.753",
    :x2 "198.656",
    :y2 "270.193",
    :x3 "202.875",
    :y3 "270.193"}]
  [:C
   {:x1 "207.098",
    :y1 "270.193",
    :x2 "207.098",
    :y2 "278.753",
    :x3 "211.318",
    :y3 "278.753"}]
  [:C
   {:x1 "215.539",
    :y1 "278.753",
    :x2 "215.539",
    :y2 "270.193",
    :x3 "219.761",
    :y3 "270.193"}]
  [:C
   {:x1 "223.982",
    :y1 "270.193",
    :x2 "223.982",
    :y2 "278.753",
    :x3 "228.205",
    :y3 "278.753"}]
  [:C
   {:x1 "232.426",
    :y1 "278.753",
    :x2 "232.426",
    :y2 "270.193",
    :x3 "236.649",
    :y3 "270.193"}]
  [:C
   {:x1 "240.872",
    :y1 "270.193",
    :x2 "240.872",
    :y2 "278.753",
    :x3 "245.095",
    :y3 "278.753"}]
  [:C
   {:x1 "248.093",
    :y1 "278.753",
    :x2 "248.963",
    :y2 "274.442",
    :x3 "250.729",
    :y3 "271.937"}]]
 [:GROUP
  {:sid "p1_s729",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5",
   :closed "true"}
  [:M {:x "209.902", :y "179.464"}]
  [:C
   {:x1 "209.902",
    :y1 "180.843",
    :x2 "208.784",
    :y2 "181.957",
    :x3 "207.407",
    :y3 "181.957"}]
  [:C
   {:x1 "206.027",
    :y1 "181.957",
    :x2 "204.913",
    :y2 "180.843",
    :x3 "204.913",
    :y3 "179.464"}]
  [:C
   {:x1 "204.913",
    :y1 "178.086",
    :x2 "206.027",
    :y2 "176.968",
    :x3 "207.407",
    :y3 "176.968"}]
  [:C
   {:x1 "208.784",
    :y1 "176.968",
    :x2 "209.902",
    :y2 "178.086",
    :x3 "209.902",
    :y3 "179.464"}]]
 [:GROUP
  {:sid "p1_s732",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5",
   :closed "true"}
  [:M {:x "168.366", :y "189.525"}]
  [:C
   {:x1 "168.366",
    :y1 "190.904",
    :x2 "167.249",
    :y2 "192.02",
    :x3 "165.871",
    :y3 "192.02"}]
  [:C
   {:x1 "164.493",
    :y1 "192.02",
    :x2 "163.377",
    :y2 "190.904",
    :x3 "163.377",
    :y3 "189.525"}]
  [:C
   {:x1 "163.377",
    :y1 "188.148",
    :x2 "164.493",
    :y2 "187.03",
    :x3 "165.871",
    :y3 "187.03"}]
  [:C
   {:x1 "167.249",
    :y1 "187.03",
    :x2 "168.366",
    :y2 "188.148",
    :x3 "168.366",
    :y3 "189.525"}]]
 [:GROUP
  {:sid "p1_s734",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.67223;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.67223;",
   :clipZone "p1_c5"}
  [:M {:x "134.419", :y "116.236"}]
  [:L {:x "269.281", :y "116.236"}]
  [:C
   {:x1 "275.471",
    :y1 "116.236",
    :x2 "280.49",
    :y2 "121.167",
    :x3 "280.49",
    :y3 "127.251"}]
  [:L {:x "280.49", :y "329.088"}]
  [:C
   {:x1 "280.49",
    :y1 "335.17",
    :x2 "275.471",
    :y2 "340.101",
    :x3 "269.281",
    :y3 "340.101"}]
  [:L {:x "94.4004", :y "340.101"}]
  [:C
   {:x1 "88.209",
    :y1 "340.101",
    :x2 "83.1907",
    :y2 "335.17",
    :x3 "83.1907",
    :y3 "329.088"}]
  [:L {:x "83.1907", :y "127.251"}]
  [:C
   {:x1 "83.1907",
    :y1 "121.167",
    :x2 "88.209",
    :y2 "116.236",
    :x3 "94.4004",
    :y3 "116.236"}]
  [:L {:x "130.278", :y "116.236"}]]
 [:GROUP
  {:sid "p1_s735",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "121.822", :y "99.4382"}]
  [:L {:x "121.822", :y "80.4866"}]]
 [:GROUP
  {:sid "p1_s736",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "119.186", :y "98.6692"}]
  [:L {:x "121.815", :y "103.223"}]
  [:L {:x "124.444", :y "98.6692"}]]
 [:GROUP
  {:sid "p1_s737",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5",
   :closed "true"}
  [:M {:x "134.332", :y "123.343"}]
  [:C
   {:x1 "134.332",
    :y1 "124.332",
    :x2 "134.227",
    :y2 "125.131",
    :x3 "134.1",
    :y3 "125.131"}]
  [:L {:x "130.582", :y "125.131"}]
  [:C
   {:x1 "130.453",
    :y1 "125.131",
    :x2 "130.349",
    :y2 "124.332",
    :x3 "130.349",
    :y3 "123.343"}]
  [:L {:x "130.349", :y "98.7901"}]
  [:C
   {:x1 "130.349",
    :y1 "97.8022",
    :x2 "130.453",
    :y2 "97.0016",
    :x3 "130.582",
    :y3 "97.0016"}]
  [:L {:x "134.1", :y "97.0016"}]
  [:C
   {:x1 "134.227",
    :y1 "97.0016",
    :x2 "134.332",
    :y2 "97.8022",
    :x3 "134.332",
    :y3 "98.7901"}]
  [:L {:x "134.332", :y "123.343"}]]
 [:GROUP
  {:sid "p1_s738",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 2.25787;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 2.25787;",
   :clipZone "p1_c5"}
  [:M {:x "109.453", :y "102.583"}]
  [:C
   {:x1 "110.052",
    :y1 "102.583",
    :x2 "109.217",
    :y2 "109.053",
    :x3 "109.543",
    :y3 "111.651"}]
  [:C
   {:x1 "110.126",
    :y1 "116.319",
    :x2 "110.512",
    :y2 "122.846",
    :x3 "111.593",
    :y3 "122.846"}]
  [:C
   {:x1 "113.274",
    :y1 "122.846",
    :x2 "113.274",
    :y2 "107.031",
    :x3 "114.954",
    :y3 "107.031"}]
  [:C
   {:x1 "116.636",
    :y1 "107.031",
    :x2 "116.636",
    :y2 "122.846",
    :x3 "118.317",
    :y3 "122.846"}]
  [:C
   {:x1 "119.999",
    :y1 "122.846",
    :x2 "119.999",
    :y2 "107.031",
    :x3 "121.681",
    :y3 "107.031"}]
  [:C
   {:x1 "123.363",
    :y1 "107.031",
    :x2 "123.363",
    :y2 "122.846",
    :x3 "125.044",
    :y3 "122.846"}]
  [:C
   {:x1 "126.727",
    :y1 "122.846",
    :x2 "126.727",
    :y2 "107.031",
    :x3 "128.41",
    :y3 "107.031"}]
  [:C
   {:x1 "129.537",
    :y1 "107.031",
    :x2 "129.501",
    :y2 "114.115",
    :x3 "130.129",
    :y3 "118.801"}]
  [:C
   {:x1 "130.439",
    :y1 "121.115",
    :x2 "129.59",
    :y2 "125.96",
    :x3 "130.147",
    :y3 "125.96"}]]
 [:GROUP
  {:sid "p1_s740",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "121.822", :y "137.173"}]
  [:L {:x "121.822", :y "123.955"}]]
 [:GROUP
  {:sid "p1_s741",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "119.185", :y "136.403"}]
  [:L {:x "121.815", :y "140.958"}]
  [:L {:x "124.444", :y "136.403"}]]
 [:GROUP
  {:sid "p1_s746",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "205.38", :y "314.167"}]
  [:L {:x "205.38", :y "307.82"}]
  [:L {:x "215.401", :y "307.82"}]]
 [:GROUP
  {:sid "p1_s747",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "214.95", :y "309.373"}]
  [:L {:x "217.631", :y "307.825"}]
  [:L {:x "214.95", :y "306.276"}]]
 [:GROUP
  {:sid "p1_s748",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.67223;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.67223;",
   :clipZone "p1_c5"}
  [:M {:x "172.665", :y "317.447"}]
  [:C
   {:x1 "174.436",
    :y1 "319.95",
    :x2 "175.302",
    :y2 "324.283",
    :x3 "178.306",
    :y3 "324.283"}]
  [:C
   {:x1 "182.526",
    :y1 "324.283",
    :x2 "182.526",
    :y2 "315.721",
    :x3 "186.746",
    :y3 "315.721"}]
  [:C
   {:x1 "190.968",
    :y1 "315.721",
    :x2 "190.968",
    :y2 "324.283",
    :x3 "195.189",
    :y3 "324.283"}]
  [:C
   {:x1 "199.41",
    :y1 "324.283",
    :x2 "199.41",
    :y2 "315.721",
    :x3 "203.63",
    :y3 "315.721"}]
  [:C
   {:x1 "207.854",
    :y1 "315.721",
    :x2 "207.854",
    :y2 "324.283",
    :x3 "212.077",
    :y3 "324.283"}]
  [:C
   {:x1 "216.298",
    :y1 "324.283",
    :x2 "216.298",
    :y2 "315.721",
    :x3 "220.518",
    :y3 "315.721"}]
  [:C
   {:x1 "224.743",
    :y1 "315.721",
    :x2 "224.743",
    :y2 "324.283",
    :x3 "228.966",
    :y3 "324.283"}]]
 [:GROUP
  {:sid "p1_s749",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.67223;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.67223;",
   :clipZone "p1_c5"}
  [:M {:x "171.904", :y "324.283"}]
  [:C
   {:x1 "176.126",
    :y1 "324.283",
    :x2 "176.126",
    :y2 "315.721",
    :x3 "180.347",
    :y3 "315.721"}]
  [:C
   {:x1 "184.57",
    :y1 "315.721",
    :x2 "184.57",
    :y2 "324.283",
    :x3 "188.79",
    :y3 "324.283"}]
  [:C
   {:x1 "193.01",
    :y1 "324.283",
    :x2 "193.01",
    :y2 "315.721",
    :x3 "197.231",
    :y3 "315.721"}]
  [:C
   {:x1 "201.454",
    :y1 "315.721",
    :x2 "201.454",
    :y2 "324.283",
    :x3 "205.676",
    :y3 "324.283"}]
  [:C
   {:x1 "209.898",
    :y1 "324.283",
    :x2 "209.898",
    :y2 "315.721",
    :x3 "214.119",
    :y3 "315.721"}]
  [:C
   {:x1 "218.343",
    :y1 "315.721",
    :x2 "218.343",
    :y2 "324.283",
    :x3 "222.565",
    :y3 "324.283"}]
  [:C
   {:x1 "226.793",
    :y1 "324.283",
    :x2 "226.793",
    :y2 "315.721",
    :x3 "231.018",
    :y3 "315.721"}]]
 [:GROUP
  {:sid "p1_s750",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.67223;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.67223;",
   :clipZone "p1_c5"}
  [:M {:x "136.365", :y "315.721"}]
  [:C
   {:x1 "140.586",
    :y1 "315.721",
    :x2 "140.586",
    :y2 "324.283",
    :x3 "144.808",
    :y3 "324.283"}]
  [:C
   {:x1 "149.03",
    :y1 "324.283",
    :x2 "149.03",
    :y2 "315.721",
    :x3 "153.25",
    :y3 "315.721"}]
  [:C
   {:x1 "157.471",
    :y1 "315.721",
    :x2 "157.471",
    :y2 "324.283",
    :x3 "161.692",
    :y3 "324.283"}]
  [:C
   {:x1 "165.912",
    :y1 "324.283",
    :x2 "165.912",
    :y2 "315.721",
    :x3 "170.134",
    :y3 "315.721"}]
  [:C
   {:x1 "174.355",
    :y1 "315.721",
    :x2 "174.355",
    :y2 "324.283",
    :x3 "178.579",
    :y3 "324.283"}]
  [:C
   {:x1 "182.801",
    :y1 "324.283",
    :x2 "182.801",
    :y2 "315.721",
    :x3 "187.021",
    :y3 "315.721"}]
  [:C
   {:x1 "191.247",
    :y1 "315.721",
    :x2 "191.247",
    :y2 "324.283",
    :x3 "195.471",
    :y3 "324.283"}]]
 [:GROUP
  {:sid "p1_s751",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.67223;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.67223;",
   :clipZone "p1_c5"}
  [:M {:x "138.407", :y "324.283"}]
  [:C
   {:x1 "142.629",
    :y1 "324.283",
    :x2 "142.629",
    :y2 "315.721",
    :x3 "146.849",
    :y3 "315.721"}]
  [:C
   {:x1 "151.073",
    :y1 "315.721",
    :x2 "151.073",
    :y2 "324.283",
    :x3 "155.291",
    :y3 "324.283"}]
  [:C
   {:x1 "159.513",
    :y1 "324.283",
    :x2 "159.513",
    :y2 "315.721",
    :x3 "163.734",
    :y3 "315.721"}]
  [:C
   {:x1 "167.956",
    :y1 "315.721",
    :x2 "167.956",
    :y2 "324.283",
    :x3 "172.179",
    :y3 "324.283"}]
  [:C
   {:x1 "176.402",
    :y1 "324.283",
    :x2 "176.402",
    :y2 "315.721",
    :x3 "180.621",
    :y3 "315.721"}]
  [:C
   {:x1 "184.847",
    :y1 "315.721",
    :x2 "184.847",
    :y2 "324.283",
    :x3 "189.071",
    :y3 "324.283"}]
  [:C
   {:x1 "192.067",
    :y1 "324.283",
    :x2 "192.937",
    :y2 "319.971",
    :x3 "194.701",
    :y3 "317.466"}]]
 [:GROUP
  {:sid "p1_s758",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5",
   :closed "true"}
  [:M {:x "141.467", :y "140.24"}]
  [:C
   {:x1 "141.467",
    :y1 "141.619",
    :x2 "140.35",
    :y2 "142.733",
    :x3 "138.972",
    :y3 "142.733"}]
  [:C
   {:x1 "137.594",
    :y1 "142.733",
    :x2 "136.478",
    :y2 "141.619",
    :x3 "136.478",
    :y3 "140.24"}]
  [:C
   {:x1 "136.478",
    :y1 "138.861",
    :x2 "137.594",
    :y2 "137.745",
    :x3 "138.972",
    :y3 "137.745"}]
  [:C
   {:x1 "140.35",
    :y1 "137.745",
    :x2 "141.467",
    :y2 "138.861",
    :x3 "141.467",
    :y3 "140.24"}]]
 [:GROUP
  {:sid "p1_s761",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "156.368", :y "148.444"}]
  [:L {:x "168.675", :y "148.444"}]]
 [:GROUP
  {:sid "p1_s762",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "167.378", :y "151.083"}]
  [:L {:x "171.933", :y "148.453"}]
  [:L {:x "167.378", :y "145.823"}]]
 [:GROUP
  {:sid "p1_s763",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "157.664", :y "145.807"}]
  [:L {:x "153.11", :y "148.436"}]
  [:L {:x "157.664", :y "151.066"}]]
 [:GROUP
  {:sid "p1_s764",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "130.082", :y "145.215"}]
  [:C
   {:x1 "126.763",
    :y1 "142.255",
    :x2 "114.829",
    :y2 "142.828",
    :x3 "111.923",
    :y3 "146.086"}]]
 [:GROUP
  {:sid "p1_s765",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "127.852", :y "146.924"}]
  [:L {:x "133.105", :y "147.184"}]
  [:L {:x "130.704", :y "142.505"}]]
 [:GROUP
  {:sid "p1_s766",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "120.474", :y "177.48"}]
  [:L {:x "135.072", :y "158.281"}]]
 [:GROUP
  {:sid "p1_s767",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "118.84", :y "175.27"}]
  [:L {:x "118.175", :y "180.487"}]
  [:L {:x "123.026", :y "178.454"}]]
 [:GROUP
  {:sid "p1_s769",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5",
   :closed "true"}
  [:M {:x "196.716", :y "142.367"}]
  [:C
   {:x1 "196.716",
    :y1 "143.746",
    :x2 "195.598",
    :y2 "144.861",
    :x3 "194.221",
    :y3 "144.861"}]
  [:C
   {:x1 "192.842",
    :y1 "144.861",
    :x2 "191.726",
    :y2 "143.746",
    :x3 "191.726",
    :y3 "142.367"}]
  [:C
   {:x1 "191.726",
    :y1 "140.99",
    :x2 "192.842",
    :y2 "139.872",
    :x3 "194.221",
    :y3 "139.872"}]
  [:C
   {:x1 "195.598",
    :y1 "139.872",
    :x2 "196.716",
    :y2 "140.99",
    :x3 "196.716",
    :y3 "142.367"}]]
 [:GROUP
  {:sid "p1_s771",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "135.622", :y "207.19"}]
  [:L {:x "153.731", :y "207.19"}]]
 [:GROUP
  {:sid "p1_s772",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "152.434", :y "209.829"}]
  [:L {:x "156.989", :y "207.198"}]
  [:L {:x "152.434", :y "204.568"}]]
 [:GROUP
  {:sid "p1_s773",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "189.008", :y "194.478"}]
  [:L {:x "201.552", :y "190.303"}]]
 [:GROUP
  {:sid "p1_s774",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "188.906", :y "191.732"}]
  [:L {:x "185.416", :y "195.665"}]
  [:L {:x "190.566", :y "196.723"}]]
 [:GROUP
  {:sid "p1_s775",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "207.515", :y "160.109"}]
  [:L {:x "213.794", :y "179.279"}]]
 [:GROUP
  {:sid "p1_s776",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "210.26", :y "160.02"}]
  [:L {:x "206.341", :y "156.511"}]
  [:L {:x "205.262", :y "161.657"}]]
 [:GROUP
  {:sid "p1_s778",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "236.328", :y "201.204"}]
  [:C
   {:x1 "232.239",
    :y1 "201.925",
    :x2 "223.279",
    :y2 "196.922",
    :x3 "222.694",
    :y3 "193.592"}]]
 [:GROUP
  {:sid "p1_s779",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "235.331", :y "198.579"}]
  [:L {:x "239.936", :y "201.122"}]
  [:L {:x "235.43", :y "203.838"}]]
 [:GROUP
  {:sid "p1_s780",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "229.262", :y "185.412"}]
  [:C
   {:x1 "233.352",
    :y1 "184.693",
    :x2 "242.314",
    :y2 "189.696",
    :x3 "242.9",
    :y3 "193.025"}]]
 [:GROUP
  {:sid "p1_s781",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "230.26", :y "188.039"}]
  [:L {:x "225.656", :y "185.496"}]
  [:L {:x "230.159", :y "182.78"}]]
 [:GROUP
  {:sid "p1_s782",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "134.304", :y "243.453"}]
  [:L {:x "125.394", :y "216.26"}]]
 [:GROUP
  {:sid "p1_s783",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "131.557", :y "243.544"}]
  [:L {:x "135.474", :y "247.052"}]
  [:L {:x "136.555", :y "241.906"}]]
 [:GROUP
  {:sid "p1_s784",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "225.906", :y "244.404"}]
  [:L {:x "245.766", :y "208.179"}]]
 [:GROUP
  {:sid "p1_s785",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "223.964", :y "242.461"}]
  [:L {:x "224.081", :y "247.718"}]
  [:L {:x "228.578", :y "244.99"}]]
 [:GROUP
  {:sid "p1_s791",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.0866;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.0866;",
   :clipZone "p1_c5"}
  [:M {:x "238.028", :y "180.116"}]
  [:L {:x "258.811", :y "144.369"}]]
 [:GROUP
  {:sid "p1_s792",
   :style "fill: #221E1F;fill-opacity: 1;",
   :clipZone "p1_c5"}
  [:M {:x "236.134", :y "178.125"}]
  [:L {:x "236.119", :y "183.384"}]
  [:L {:x "240.682", :y "180.768"}]]
 [:CLIP
  {:sid "p1_s793",
   :x "71.91",
   :y "72.3683",
   :width "220.035",
   :height "268.278",
   :idClipZone "p1_c6"}
  [:GROUP
   {:sid "p1_s794", :closed "true"}
   [:M {:x "71.91", :y "72.3683"}]
   [:L {:x "291.945", :y "72.3683"}]
   [:L {:x "291.945", :y "340.646"}]
   [:L {:x "71.91", :y "340.646"}]
   [:L {:x "71.91", :y "72.3683"}]]]
 [:CLIP
  {:sid "p1_s795",
   :x "71.91",
   :y "72.3683",
   :width "220.035",
   :height "268.278",
   :idClipZone "p1_c7"}
  [:GROUP
   {:sid "p1_s796", :closed "true"}
   [:M {:x "21", :y "777"}]
   [:L {:x "591", :y "777"}]
   [:L {:x "591", :y "15"}]
   [:L {:x "21", :y "15"}]
   [:L {:x "21", :y "777"}]]]
 [:GROUP
  {:sid "p1_s797",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.67223;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.67223;",
   :clipZone "p1_c7"}
  [:M {:x "71.91", :y "116.235"}]
  [:C
   {:x1 "78.2493",
    :y1 "116.235",
    :x2 "83.3866",
    :y2 "121.167",
    :x3 "83.3866",
    :y3 "127.25"}]
  [:L {:x "83.3866", :y "329.088"}]
  [:C
   {:x1 "83.3866",
    :y1 "335.17",
    :x2 "78.2493",
    :y2 "340.101",
    :x3 "71.91",
    :y3 "340.101"}]]
 [:GROUP
  {:sid "p1_s798",
   :style
   "stroke: #221E1F;fill:none;stroke-width: 1.67223;stroke-linejoin:miter;stroke-linecap:butt;stroke-width: 1.67223;",
   :clipZone "p1_c7"}
  [:M {:x "291.965", :y "340.101"}]
  [:C
   {:x1 "285.628",
    :y1 "340.101",
    :x2 "280.49",
    :y2 "335.17",
    :x3 "280.49",
    :y3 "329.088"}]
  [:L {:x "280.49", :y "127.251"}]
  [:C
   {:x1 "280.49",
    :y1 "121.168",
    :x2 "285.628",
    :y2 "116.235",
    :x3 "291.965",
    :y3 "116.235"}]]
 [:CLIP
  {:sid "p1_s799",
   :x "21",
   :y "15",
   :width "570",
   :height "762",
   :idClipZone "p1_c8"}
  [:GROUP
   {:sid "p1_s800", :closed "true"}
   [:M {:x "21", :y "777"}]
   [:L {:x "591", :y "777"}]
   [:L {:x "591", :y "15"}]
   [:L {:x "21", :y "15"}]
   [:L {:x "21", :y "777"}]]]])

