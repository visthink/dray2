(ns drae.util.exec
  "Utilities for calling external programs."
  (:import (java.lang System))
  (:require [clojure.java.io :refer [file]]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as s]
            [drae.util :refer [uerr]]
            )
  )

;;; FIND USER HOME DIRECTORY

(defn home-dir 
  "User home directory (as file)." 
  [] 
  (file (java.lang.System/getenv "HOME")))

;;; DETERMINE OS (HEURISTIC)

(defn- os-type-unmemoized 
  "Attempts to determine the type of OS based on the value of System/getProperty(os.name)."
  []
  (let [os-name (s/lower-case (System/getProperty "os.name"))
        found? (fn [^String target ^String s] (>= (.indexOf s target) 0))]
    (cond (found? "mac" os-name)    :mac
          (found? "win" os-name)    :windows
          (found? "nix" os-name)    :unix
          :else :unknown)))

(def os-type 
  "Attempts to determine the type of OS based on the value of System/getProperty(os.name).
   Returns either :windows, :mac, :unix or :unknown. Memoized."
  (memoize os-type-unmemoized))

;;; DEFAULT ADDITIONAL PATHS

(def ^{:dynamic true} *additional-exec-paths* 
  "Additional paths to check for executables. "
  (list "/usr/local/bin" (file (home-dir) "bin")))


;;; ADD PATHS TO DEFAULT PATH

(defn add-to-system-path 
  "Return a string of paths that appends the path strings to the current system path.
   Does not actually reset the system path."
  [& path-strings]
  (apply str (interpose ":" (cons (System/getenv "PATH") path-strings))))

;;; LOCATING EXECUTABLES IN UNIX

(defn unix-which 
  "Attempt to find the given command name using the unix `which` command. 
   If error-p is true and the command is not found, an exception is thrown.
   If error-p is false and error-code is true, returns that code if the command can not be found.
   By default, error-p is false and the error code is a null string.
   If the external call to `which` fails, an exception is thrown. Returns nil when called 
   from Windows."
  ([cmd-name error-p error-code] 
    (if (= :windows (os-type)) nil ;; Can't do Windows
      (let [res (try (sh "which" cmd-name)
                  (catch Exception e (str "Call to unix which failed with message: " (.getMessage e))))]
        (cond (not (zero? (:exit res)))
              (if error-p (uerr "Unix-which function could not find command: %s." cmd-name) error-code)
              :else (s/trim (:out res))))))
  ([cmd-name error-p] (unix-which cmd-name error-p nil))
  ([cmd-name] (unix-which cmd-name false nil)))

