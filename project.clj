(defproject org.clojars.dropthe/benchgecko "0.1.0"
  :description "Clojure SDK for BenchGecko — compare LLM benchmarks, estimate inference costs, and explore AI model performance data"
  :url "https://benchgecko.ai"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :scm {:name "git"
        :url "https://github.com/BenchGecko/benchgecko-clojure"}
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :source-paths ["src"]
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                     :username :env/clojars_username
                                     :password :env/clojars_password}]])
