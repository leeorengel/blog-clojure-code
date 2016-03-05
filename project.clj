(defproject blog-post-code "0.1.0-SNAPSHOT"
  :description "Clojure code from my blog posts"
  :url "https://github.com/leeorengel/blog-clojure-code"
  :min-lein-version "2.5.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [medley "0.7.3"]
                 [org.clojure/test.check "0.9.0"]
                 [com.gfredericks/test.chuck "0.2.6"]
                 [prismatic/schema "1.0.4"]]
  :target-path "target/%s")
