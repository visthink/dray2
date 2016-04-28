(ns dray.domain.recognizers
  "Facilities for creating recognizer test domains for recognizing column, row, and element types."
  (:require [dray.util :refer [uerr]])
  )

;; HELP ROUTINES

(defn check-arg [arg-name valid-set x]
  (if-not (contains? valid-set x)
    (uerr IllegalArgumentException "%s argument is %s, but must be one of %s"
          arg-name x valid-set)))

(def check-arg-style (partial check-arg 'arg-style #{:single :parwise :group}))

(def check-return-style (partial check-arg 'return-style #{:boolean :bayesian}))

;;; RECOGNIZER TEST RECORD

(defrecord RecognizerTest [name doc arg-style return-style rec-fn]
  Object
  (toString [this] (format "<RecognizerTest: %s>" (:name this))))

(defmethod print-method RecognizerTest [x writer]
  (.write writer (.toString x)))

(defn make-recognizer-test [name doc rec-fn & {:keys [arg-style, return-style]
                                               :or {arg-style :single, return-style :boolean}}]
  (check-arg-style arg-style)
  (check-return-style return-style)
  (->RecognizerTest name doc arg-style return-style rec-fn))

;;; DefRecogizerTest macro

(defmacro defRecognizerTest [name doc rec-fn & {:keys [arg-style return-style]
                                                :or {arg-style :single, return-style :boolean}}]
  `(make-recognizer-test '~name ~doc ~arg-style ~return-style ~rec-fn))


;;; Pattern matchers

(def pattern-matching-tests
  {:IPI-numbers
   {:regex #"IPI.{8}"
    :goods ["IPI00607670,IPI00005495" "IPI00150961," "IPI00030910"]
    :bads ["ataxin 3---(ATXN3 protein)" "IPIIDIDDDDDfff" "IPI00607670000"]}
   })

(defn run-test-pattern [regex-key & {:keys [verbose?] :or {verbose? true}}]
  (let [m (get pattern-matching-tests regex-key)
        {:keys [regex goods bads]} m]
    (if verbose?
      (println (format "For %s pattern:" regex-key)))
    (doseq [good goods]
      (if-not (re-matches regex good)
        (println (format "!! Could not match %s to good value %s." regex good))
        (if verbose? (println (format "Correctly matched %s to good value %s." regex good)))))
    (doseq [bad bads]
      (if (re-matches regex bad)
        (println (format "!! Regex %s matched to %s, but it should not." regex bad))
        (if verbose? (println (format "Regex %s properly rejected match to %s." regex bad))))))) 

         
  
