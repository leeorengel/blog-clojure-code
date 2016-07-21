(ns prop-testing-spec-test-check.core-test
  ^{:author "Leeor Engel"}
  (:require [prop-testing-spec-test-check.core :as core]
            [clojure.test :refer :all]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as tcg]
            [clojure.test.check.clojure-test :refer :all]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.spec.test :as stest]))

(defn notes-gen [size] (gen/vector (s/gen ::core/note) size))
(s/fdef notes-gen
        :args (s/cat :size pos-int?)
        :ret tcg/generator?)

(defn rests-gen [size] (gen/vector (s/gen ::core/rest) size))
(s/fdef rests-gen
        :args (s/cat :size pos-int?)
        :ret tcg/generator?)

(defn notes-and-rests-gen [size num-notes]
  (gen/bind (notes-gen num-notes) (fn [v]
                                    (let [remaining (- size num-notes)]
                                      (if (zero? remaining)
                                        (gen/return v)
                                        (gen/fmap (fn [rests] (shuffle (into v rests))) (rests-gen remaining)))))))

(defn melody-gen [size num-notes]
  (s/gen ::core/melody {::core/notes #(notes-and-rests-gen size num-notes)}))

;(stest/check `with-new-notes {:clojure.spec.test.check/opts {:num-tests 20}})

(defspec with-new-notes-test-check 1000
         (let [test-gens (tcg/let [num-notes tcg/s-pos-int
                                   melody-num-rests tcg/s-pos-int
                                   total-melody-num-notes (tcg/return (+ num-notes melody-num-rests))
                                   melody (melody-gen total-melody-num-notes num-notes)
                                   new-notes (notes-gen num-notes)]
                                  [melody new-notes])]
           (prop/for-all [[melody new-notes] test-gens]
                         (let [new-melody (with-new-notes melody new-notes)]
                           (= (count new-notes) (note-count (:notes new-melody)))
                           (= new-notes (remove rest? (:notes new-melody)))))))

