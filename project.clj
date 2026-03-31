(defproject benchgecko "0.1.0"
  :description "Official Clojure SDK for the BenchGecko API. Compare AI models, benchmarks, and pricing."
  :url "https://benchgecko.ai"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-http "3.12.3"]
                 [cheshire "5.12.0"]]
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :username "dropthe"
                                    :password :env/CLOJARS_TOKEN}]]
  :scm {:name "git"
        :url "https://github.com/BenchGecko/benchgecko-clojure"})
