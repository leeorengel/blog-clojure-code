(ns prop-testing-spec-test-check.core-test
  ^{:author "Leeor Engel"}
  (:require [prop-testing-spec-test-check.core :as core]
            [prop-testing-spec-test-check.core :refer :all]
            [clojure.test :refer :all]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as tcg]
            [clojure.test.check.clojure-test :refer :all] 
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.spec.test :as stest]))

(stest/instrument (stest/enumerate-namespace 'prop-testing-spec-test-check.core))

(def RestGenerator (gen/return -1))
(s/def ::note-or-rest-gen-args (s/cat :size pos-int?))

(defn notes-gen [size] (gen/vector (s/gen ::core/note) size))
(s/fdef notes-gen
        :args (s/cat :size pos-int?)
        :ret tcg/generator?)

(defn rests-gen [size] (gen/vector RestGenerator size))
(s/fdef rests-gen
        :args ::note-or-rest-gen-args
        :ret tcg/generator?)

;;(->> (stest/check `notes-gen) (stest/summarize-results))
;;(->> (stest/check ::core/rest?) (stest/summarize-results))

(defn notes-and-rests-gen [size num-notes]
  (gen/bind (notes-gen num-notes) (fn [v]
                                    (let [remaining (- size num-notes)]
                                      (if (zero? remaining)
                                        (gen/return v)
                                        (gen/fmap (fn [rests] (shuffle (into v rests))) (rests-gen remaining)))))))

(defn melody-gen [size num-notes]
  (s/gen ::core/melody {::core/notes (notes-and-rests-gen size num-notes)}))

(defspec with-new-notes-test-check 1000
  (let [test-gens (tcg/let [num-notes tcg/s-pos-int
                            melody-num-rests tcg/s-pos-int
                            total-melody-num-notes (tcg/return (+ num-notes melody-num-rests))
                            melody (melody-gen total-melody-num-notes num-notes)
                            notes (notes-gen num-notes)]
                    [melody notes])]
    (prop/for-all [[melody notes] test-gens]
                  (let [new-melody (with-new-notes melody notes)]
                    (= (count notes) (note-count (:notes new-melody)))
                    (= notes (remove rest? (:notes new-melody)))))))


;; (defn simple-fn [a b] (a))
;; (s/fdef simple-fn
;;         :args (s/cat :a string? :b string?)
;;         :ret string?
;;         :fn #(= (:ret %) (:a (:args %))))

;; (stest/check `simple-fn)

;;(->> (stest/check `simple-fn {:clojure.spec.test.check/opts {:num-tests 5} }))



;; (stest/check `rest? {::stest/opts {:num-tests 2}
;;                      :gen {::core/note NoteGenerator
;;                            ::core/rest RestGenerator}})


;; (stest/check `with-new-notes {:clojure.spec.test.check/opts {:num-tests 10
;;                                                              :property (prop/for-all [[melody notes] test-gens]
;;                                                                                      (let [new-melody (with-new-notes melody notes)] 
;;                                                                                        (= notes (remove rest? (:notes new-melody)))))}
;;                               :gen { ::core/melody (first test-gens)}})

