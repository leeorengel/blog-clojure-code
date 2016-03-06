(ns prop-testing-schema-test-check.core
  ^{:author "Leeor Engel"}
  (:require [schema.core :as s]))

(def REST-NOTE-NUMBER -1)
(def MIN-NOTE 0)
(def MAX-NOTE 127)

(def Rest (s/eq REST-NOTE-NUMBER))
(def Note (s/constrained s/Int #(<= MIN-NOTE % MAX-NOTE)))
(def NoteOrRest (s/either Note Rest))
(s/defrecord Melody [notes :- [NoteOrRest]])

(s/defn rest? :- s/Bool [n :- NoteOrRest] (neg? n))
(s/defn note-count :- s/Int [notes :- [NoteOrRest]] (count (remove rest? notes)))

(s/defn with-new-notes :- Melody
  [melody :- Melody new-notes :- [Note]]
  {:pre [(= (count new-notes) (note-count (:notes melody)))]}
  (let [notes (first (reduce (fn [[updated-notes new-notes] note]
                               (if (rest? note)
                                 [(conj updated-notes note) new-notes]
                                 [(conj updated-notes (first new-notes)) (rest new-notes)]))
                             [[] new-notes] (:notes melody)))]
    (->Melody notes)))