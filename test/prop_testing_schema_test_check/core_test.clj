(ns prop-testing-schema-test-check.core-test
  ^{:author "Leeor Engel"}
  (:require [prop-testing-spec-test-check.core :as core]
            [prop-testing-spec-test-check.core :refer :all]
            [clojure.test :refer :all]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as tcg]
            [clojure.test.check.clojure-test :refer :all]
            [com.gfredericks.test.chuck.properties :as tcp]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]))

(s/def ::pos-int (s/and integer? pos?))

(def RestGenerator (tcg/return -1))

;; Not using the schema genator here because s/constrained will not always gaurantee valid inputs
(def NoteGenerator (gen/choose MIN-NOTE MAX-NOTE))

(defn notes-gen [size] (gen/vector NoteGenerator size))
(s/fdef notes-gen
        :args ::pos-int
        :ret tcg/generator?)

(defn rests-gen [size] (gen/vector RestGenerator size))
(s/fdef rests-gen
        :args ::pos-int
        :ret tcg/generator?)

(defn notes-and-rests-gen [size num-notes]
  (gen/bind (notes-gen num-notes) (fn [v]
                                    (let [remaining (- size num-notes)]
                                      (if (zero? remaining)
                                        (gen/return v)
                                        (gen/fmap (fn [rests] (shuffle (into v rests))) (rests-gen remaining)))))))

(defn melody-gen [size num-notes]
  (s/gen ::core/melody {::core/notes (notes-and-rests-gen size num-notes)}))

;;
;; test.check version
;;

(defspec with-new-notes-test-check 1000
  (let [test-gens (tcg/let [num-notes tcg/s-pos-int
                            melody-num-rests tcg/s-pos-int
                            total-melody-num-notes (tcg/return (+ num-notes melody-num-rests))
                            melody (melody-gen total-melody-num-notes num-notes)
                            notes (notes-gen num-notes)
                            ]
                    [melody notes])]
    (prop/for-all [[melody notes] test-gens]
                  (let [new-melody (with-new-notes melody notes)]
                    (= (count notes) (note-count (:notes new-melody)))
                    (= notes (remove rest? (:notes new-melody)))))))

;; ;;
;; ;; test.chuck version
;; ;;

(defspec with-new-notes-test-chuck 1000
  (tcp/for-all [num-notes tcg/s-pos-int
                melody-num-rests tcg/s-pos-int
                total-melody-num-notes (gen/return (+ num-notes melody-num-rests))
                melody (melody-gen total-melody-num-notes num-notes)
                notes (notes-gen num-notes)]
               (let [new-melody (with-new-notes melody notes)]
                 (= (count notes) (note-count (:notes new-melody)))
                 (= notes (remove rest? (:notes new-melody))))))
