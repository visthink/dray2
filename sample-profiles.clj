;;;; 
;;;; (sample-)profiles.clj   User-specific settings for the DRAY project.
;;;;
;;;; *** Resave file as profiles.clj and edit as indicated below.
;;;;
;;;; Instructions: This file, if renamed to profiles.clj, sets user-specific overrides for 
;;;; running and compiling dray. It provides values for system-specific settings
;;;; that are then used by Leiningen. 
;;;;
;;;; User-wide defaults for all lein-based projects can also be set 
;;;; in the ~/.lein/profiles.clj file.

{:dev ;; Reset values for the :dev (development) profile
 
  ;; Set the location of the java executable. 
 {

  :java-cmd "/Library/Java/JavaVirtualMachines/jdk1.7.0_51.jdk/Contents/Home/bin/java" ; Mac example
  ;; :java-cmd "C:/Program Files/Java/jdk1.7.0_75/bin/java.exe" ; Windows example
 
  ;; Java runtime options. If you work with large ontologies, you may need a larger runtime memory allocation
  ; :java-opts ["-Xmx16G"]
  
  ; Java compiler options. This is probably not needed.
  ; :javac-options ["-target" "1.7" "-source" "1.7"]
  
 }
}
 