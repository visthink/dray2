;;;;
;;;; drae.lisp: Clone code for LISP remote procedure calls.
;;;;
;;;; Ron Ferguson, Leidos
;;;; Created: 2014-05-02
;;;; Updated: 2014-05-05
;;;;
;;;; This is a set of routines, based on the Conch library, that
;;;; allows Clojure to start a Lisp process, issue commands to the 
;;;; process, and get the output. In addition, it has a set of simple
;;;; commands for running commands on a default Lisp process that 
;;;; should be useful on the command line.
;;;;
;;;; Next step: Still having timing issues. Need to update to 
;;;;  permit the Lisp call to return a second random key that 
;;;;  will be used to confirm that the command has actually 
;;;;  completed on the Lisp side. Otherwise, there's no way to be sure.
;;;;
;;;;
;;;; Current issues:
;;;; The code is basically functional, but there is still a lot more
;;;; that could be done. We should add support for 
;;;; asynchronous handling of Lisp commands. Also, the system can 
;;;; be buggy for Lisp commands that are slow (e.g., calling a slow
;;;; routine for processing a diagram). Those will need to be handled, 
;;;; possibly by adding a random token to the end of the issued command
;;;; and having the Lisp process return that token with its output.
;;;;
;;;; In addition, we also don't have good commands (yet!) for testing
;;;; and checking the status of the Lisp process.
;;;;
;;;; History:
;;;;  2014-05-05: Initial version completed.
;;;;
(ns drae.ext.lisp
  "This is a set of routines, based on the Conch library, that
   allows Clojure to start a Lisp process, issue commands to the 
   process, and get the output. In addition, it has a set of simple
   commands for running commands on a default Lisp process that 
   should be useful on the command line.

   The current version is still having timing issues. Need to update module to 
   permit the Lisp call to return a second random key that 
   will be used to confirm that the command has  
   completed on the Lisp side."
  
  (:import (java.io CharArrayWriter InputStreamReader BufferedReader)
           (java.lang ProcessBuilder)
           (java.util Scanner))
  (:require [clojure.java.shell :as old-shell]
            [clojure.repl :refer :all]
            [clojure.string :as string]
            [me.raynes.conch :refer :all] ;; Trying this out.
            [me.raynes.conch.low-level :as sh]))

;;; This is some experimental code for calling remost procedures.

(def ^:dynamic *lisp-exe* "/usr/local/bin/sbcl")
(def ^:dynamic *proc* nil)
(def ^:dynamic *tiny-repl-prog* "(loop (print (eval (read))) (terpri))\n\n")

(defn add-output-buffer
  "Add output buffer slot for Lisp process. Needed for the other routines."
  [proc]
  (assoc proc :output-buffer (BufferedReader. (InputStreamReader. (:out proc)))))

(defn lisp-read-line 
  "Read a single line from the lisp process output buffer. If the read is blocked
   for over 1000 milliseconds, returns :timeout. If not yet ready,
   returns nil."
  [p]
  (let [b (:output-buffer p)]
    (deref (future (when (.ready b) (.readLine b))) 500 :timeout)))

(defn lisp-read-lines [p]
  (take-while #(not (or (= % :timeout) (nil? %))) (repeatedly 100 #(lisp-read-line p))))

(defn lisp-start "Start and return a Lisp process." [] 
  (let [p (add-output-buffer (sh/proc *lisp-exe*))]
    (dorun (map println (lisp-read-lines p)))
    p))

(defn lisp-kill "Kill the Lisp process." [p] (sh/destroy p))

(defn lisp-initilize [sbcl-proc]
  (sh/feed-from-string sbcl-proc "\n(terpri)\n")
  (sh/feed-from-string sbcl-proc *tiny-repl-prog*))

(defn lisp-flush-output [p]
  (map println (lisp-read-lines p)))

(defn lisp-issue-command [p command]
  (sh/feed-from-string p (str command "\n"))
  p)

(defn lisp-command- 
  "Run a Lisp command and return output as string list."
  [p command]
  (lisp-read-lines (lisp-issue-command p command)))

(defn lisp-command "Run a Lisp command and return resulting s-expression."
  [p command]
  (read-string (apply str (lisp-command- p command))))

(defn lisp-test 
  "Run a test command in the Lisp process and check that it returns the
   proper value. If no argument, will add two random values. If one
   argument, will run same command on Lisp and Clojure and compare the result.
   If two arguments, will run the first in Lisp and compare to the result
   of the second in Clojure."
  ([p] (lisp-test p `(+ ~(rand-int 100) ~(rand-int 100))))
  ([p expr] (lisp-test p expr expr))
  ([p lisp-expr clojure-expr]
     (lisp-flush-output p)
     (let [lisp-out (lisp-command p lisp-expr)
           clj-out (eval clojure-expr)
           same? (= lisp-out clj-out)]
       (when-not same?
         (println "Lisp test failed: " lisp-out " does not equal " clj-out "."))
       same?)))

;;; Define a set of macros to allow a semblance of normal Lisp interaction.

(defmacro l-start []
  `(def ^:dynamic *proc* (lisp-start)))

(defmacro l-kill []
  `(lisp-kill *proc*))

(defmacro l [command]
  (let [c# command]
    `(lisp-command *proc* '~c#)))

(defmacro l- [command]
  (let [c# command]
    `(lisp-command- *proc* '~c#)))



