(ns drae.j.paxtools
  "Wrapper for BioPax Pattern that includes additional information.")

(defrecord XPattern [name pattern args explain-fmt])

(defrecord XMatch [start matchlist pattern])

(defrecord XMatches [pattern items]) ; Depracated.




