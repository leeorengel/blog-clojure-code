(ns prop-testing-schema-test-check.core-test
  ^{:author "Leeor Engel"}
  (:require [prop-testing-schema-test-check.core :refer :all]
            [clojure.test :refer :all]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer :all]
            [com.gfredericks.test.chuck.properties :as tcp]
            [schema.core :as s]
            [schema.test]
            [schema.experimental.generators :as sgen])
  (:import (clojure.test.check.generators Generator)
           (prop_testing_schema_test_check.core Melody)))

(use-fixtures :once schema.test/validate-schemas)

(def PositiveInt (s/constrained s/Int pos?))

;;
;; Generators
;;

(def RestGenerator (sgen/always -1))
;; Not using the schema genator here because s/constrained will not always gaurantee valid inputs
(def NoteGenerator (gen/choose MIN-NOTE MAX-NOTE))

(s/defn notes-gen :- Generator [size :- PositiveInt] (gen/vector NoteGenerator size))

(s/defn rests-gen :- Generator [size :- PositiveInt] (gen/vector RestGenerator size))

(s/defn notes-and-rests-gen :- Generator
        [size :- PositiveInt
         num-notes :- PositiveInt]
        (gen/bind (notes-gen num-notes) (fn [v]
                                          (let [remaining (- size num-notes)]
                                            (if (zero? remaining)
                                              (gen/return v)
                                              (gen/fmap (fn [rests] (shuffle (into v rests))) (rests-gen remaining)))))))

(s/defn melody-gen :- Generator
        ([size :- PositiveInt
          num-notes :- PositiveInt]
          (sgen/generator Melody {[NoteOrRest] (notes-and-rests-gen size num-notes)})))

;;
;; test.check version
;;

(defspec with-new-notes-test-check 1000
         (let [test-gens (gen/let [num-notes gen/s-pos-int
                                   melody-num-rests gen/s-pos-int
                                   total-melody-num-notes (gen/return (+ num-notes melody-num-rests))
                                   melody (melody-gen total-melody-num-notes num-notes)
                                   notes (notes-gen num-notes)]
                                  [melody notes])]
           (prop/for-all [[melody notes] test-gens]
                         (let [new-melody (with-new-notes melody notes)]
                           (and (= (count notes) (note-count (:notes new-melody)))
                                (= notes (remove rest? (:notes new-melody))))))))

;;
;; test.chuck version
;;

(defspec with-new-notes-test-chuck 1000
         (tcp/for-all [num-notes gen/s-pos-int
                       melody-num-rests gen/s-pos-int
                       total-melody-num-notes (gen/return (+ num-notes melody-num-rests))
                       melody (melody-gen total-melody-num-notes num-notes)
                       notes (notes-gen num-notes)]
                      (let [new-melody (with-new-notes melody notes)]
                        (and (= (count notes) (note-count (:notes new-melody)))
                             (= notes (remove rest? (:notes new-melody)))))))
