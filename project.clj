;;;; project.clj  -  Lein specification for the DRAE project.
;;;;
;;;; This file specifies the project dependencies using lein, a 
;;;; Clojure version of maven: http://leiningen.org/
;;;;
;;;; User-specific settings for lein may also be set in a profiles.clj file.
;;;; See the sample-profiles.clj file for more details.
;;;;
(defproject drae "0.0.9-SNAPSHOT"
  :description 
  "DRAE (Diagrammatic Reasoning and Analysis Engine) is a framework for performing graphical and 
   structural analysis of PDF and other document files. It is currently in development."
  :url "http://leidos.com"
  :license 
     {:name "Government-purpose rights"}
  :main drae.core
  :repositories 
     [["java.net" "http://download.java.net/maven/2"]
      ["biopax-releases" "http://www.biopax.org/m2repo/releases/"]
      ["biopax-snapshots" "http://www.biopax.org/m2repo/snapshots/"]
      ["pattern.repo" "http://maven-repo.biopax-pattern.googlecode.com/hg/"]
      ["sonatype-snapshots" "https://oss.sonatype.org/content/repositories/snapshots/"]
      ["apache-snapshots" "https://repository.apache.org/content/groups/snapshots/"] ] 
      ;["pathwaycards-snapshots" "C:/dev/BigMechanism/pathway-cards-master" :extension pom] 
  :min-lein-version "2.0.0"
  ; :pedantic? :warn ;; Warn on library version conflicts
  :dependencies 
     [[org.clojure/clojure "1.8.0"]
      [org.clojure/data.xml "0.0.8"]
      [org.clojure/data.json "0.2.6"]
      [junit/junit "4.12"]
   ;   [com.oracle/javafx-runtime "2.2.0"]
      [pjstadig/humane-test-output "0.7.1"]
      [commons-io/commons-io "2.4"]
      [xerces/xercesImpl "2.11.0"] ;; Test
      [org.apache.pdfbox/pdfbox "2.0.0-SNAPSHOT"]
      [log4j "1.2.17"]
      [org.clojure/tools.logging "0.3.1"]
      [me.raynes/conch "0.8.0"]
      [seesaw "1.4.5"]
      [clj-http-lite "0.3.0"]
      [org.biopax.paxtools/paxtools-core "4.3.1"]
      [org.biopax.paxtools/pattern "4.3.1"]; :exclusions [org.biopax.paxtools/paxtools-core]]
      [incanter "1.9.0"]
      [org.scala-lang/scala-library "2.11.6"]
      [args4j/args4j "2.0.25"]
      [com.google.protobuf/protobuf-java "2.6.1"]
      [org.jsoup/jsoup "1.8.3"]
      [javax.json/javax.json-api "1.0"]
      [org.glassfish/javax.json "1.0.4"]
      [junit/junit "4.12"]
      [org.hamcrest/hamcrest-all "1.3"]
      ]
  :injections 
     [(require 'pjstadig.humane-test-output)
      (pjstadig.humane-test-output/activate!)]
  :plugins 
     [[codox "0.8.11" :exclusions [leinjacker]];; Autodocumenter.
      [lein-ancient "0.5.5"]
      [lein-localrepo "0.5.3"]
      [lein-environ "1.0.0"]
      [lein-javadoc "0.2.0"]] 
  :codox 
     {:exclude [drae.ext.lisp   ;; Not used.
                drae.ext.python 
                drae.pipeline2
                drae.j.paxtools ;; Hidden record def.
                drae.test-data  ;; Hidden (not part of API).
                ] 
      :project {:name "DRAE"}
      :defaults {:doc/format :markdown}
      :output-dir "doc/API"
      }
  :eastwood {:exclude-linters [:constant-test]}
  :javadoc-opts
  {:package-names ["com.leidos.bmech.model"
                   "com.leidos.bmech.analysis"
                   "com.leidos.bmech.gui"
                   "com.leidos.bmech.view"]
   :output-dir "doc/gui/"
   :java-source-paths ["src/gui/BigMechViewer/src"]
  }
    
  ;; Platform-specific java settings - Do not uncomment unless desperate! :)
  ;;
  ;; NOTE: Please set these Java parameters using a local profiles.clj file 
  ;;       rather than by modifying this file. See sample-profiles.clj for 
  ;;       an example.
   
       ;  :java-cmd "C:/Program Files/Java/jdk1.7.0_75/bin/java.exe"
       ;  :java-opts ["-Xmx16G"]
 
  ;; Other Java options that should be cross-platform
  
  ; :javac-options ["-target" "1.8" "-source" "1.8"]
  :java-source-paths ["src/gui/BigMechViewer/src"]

  ;; Compilation: We first precompile many of the drae.j.* classes, then 
  ;;              compile the Java code, then the remaining Clojure code.
  
  :prep-tasks [
               ;;-- Clojure records and generated classes
               ["compile" 
                "drae.j.VisualElement" 
                "drae.j.Doc"
                "drae.j.paxtools"
                "drae.j.Paxll"
                "drae.j.Toys"
                "drae.j.Producer"
                ]
               ;; -- Java code compile (for GUI)
               "javac"
               ;; -- Compile remaining AOT-compilation items (just drae.core/main).
              #_ "compile"
               ]
  
  :aot [drae.core]

  :aliases {"newdoc" ["do" "clean" ["doc"] ["javadoc"]]}

  :profiles ;;;--- BEGIN PROFILES ---
  {
   :dev ;;;--- DEVELOPMENT PROFILE --- 
    {:aot [] 
     :plugins [[com.jakemccrary/lein-test-refresh "0.5.4"]]
     :dependencies
      [#_[org.clojure/tools.trace "0.7.8"]
       #_[im.chit/iroh "0.1.11"] ; Java reflection library.
       ]                               
     }
    :uberjar {:aot :all, :prep-tasks ["compile"]}
    } ;;; --- END PROFILES ---

  :test-refresh 
    {:notify-command ["say"]
     :growl false
     :notify-on-success true
     }
  :resource-paths []
  ;:javadoc-opts
  ;{:package-names ["com.leidos.bmech.gui"]
   ;;:java-source-paths "C:/Program Files/Java/jdk1.7.0_75/bin/javadoc.exe"
  ; :output-dir "javadoc-test"
  ; }
  )
