(ns prop-testing-spec-test-check.core
  ^{:author "Leeor Engel"}
  (:require [clojure.spec :as s]))

(def MIN-NOTE 0)
(def MAX-NOTE 127)

(s/def ::rest #(= % -1))
(s/def ::note #(<= MIN-NOTE % MAX-NOTE))

(s/def ::note-or-rest (s/or :note ::note :rest ::rest))

(s/def ::notes (s/and vector? (s/+ ::note-or-rest)))

(defrecord Melody [notes])

(s/def ::melody (s/keys :req-un [::notes]))

(defn rest? [n] (neg? n))
(s/fdef rest?
        :args (s/+ ::note-or-rest)
        :ret boolean?)

(defn note-count [notes] (count (remove rest? notes)))
(s/fdef note-count
        :args (s/and vector? (s/+ ::note-or-rest))
        :ret integer?
        :fn #(<= (:ret %) (-> % :args :notes count)))

(defn with-new-notes [melody new-notes] 
  (let [notes (first (reduce (fn [[updated-notes new-notes] note]
                               (if (rest? note)
                                 [(conj updated-notes note) new-notes]
                                 [(conj updated-notes (first new-notes)) (rest new-notes)]))
                             [[] new-notes] (:notes melody)))]
    (->Melody notes)))

(s/fdef with-new-notes
        :args (s/and (s/cat :melody ::melody
                            :new-notes (s/+ ::note))
                     #(= (count (:new-notes %)) (note-count (-> :melody :notes %))))
        :ret ::melody)
