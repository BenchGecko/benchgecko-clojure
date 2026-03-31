(ns benchgecko.core
  "Official Clojure SDK for the BenchGecko API.

  BenchGecko tracks every major AI model, benchmark, and provider.
  This library wraps the public REST API for querying model data,
  benchmark scores, and model comparisons.

  Usage:
    (require '[benchgecko.core :as bg])
    (bg/models)
    (bg/benchmarks)
    (bg/compare [\"gpt-4o\" \"claude-opus-4\"])"
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(def ^:dynamic *base-url*
  "Base URL for the BenchGecko API."
  "https://benchgecko.ai")

(def ^:dynamic *timeout*
  "HTTP timeout in milliseconds."
  30000)

(defn- api-request
  "Send a GET request to the BenchGecko API.
  Returns parsed JSON response body."
  [path & {:keys [params]}]
  (let [url (str *base-url* path)
        response (http/get url
                   {:query-params params
                    :headers {"User-Agent" "benchgecko-clojure/0.1.0"
                              "Accept" "application/json"}
                    :socket-timeout *timeout*
                    :connection-timeout *timeout*
                    :as :json
                    :throw-exceptions false})]
    (if (<= 200 (:status response) 299)
      (:body response)
      (throw (ex-info (str "BenchGecko API error (status " (:status response) ")")
                      {:status (:status response)
                       :body (:body response)})))))

(defn models
  "List all AI models tracked by BenchGecko.

  Returns a sequence of model maps, each containing metadata like
  name, provider, parameter count, pricing, and benchmark scores.

  Example:
    (def all-models (models))
    (count all-models)
    (map :name (take 5 all-models))"
  []
  (api-request "/api/v1/models"))

(defn benchmarks
  "List all benchmarks tracked by BenchGecko.

  Returns a sequence of benchmark maps with name, category,
  and description.

  Example:
    (def all-benchmarks (benchmarks))
    (map :name all-benchmarks)"
  []
  (api-request "/api/v1/benchmarks"))

(defn compare
  "Compare two or more AI models side by side.

  Accepts a vector of model slugs (minimum 2) and returns a
  structured comparison with per-model scores and pricing.

  Example:
    (compare [\"gpt-4o\" \"claude-opus-4\"])
    ;; => {:models [{:name \"GPT-4o\" :scores {...}} ...]}"
  [model-slugs]
  {:pre [(>= (count model-slugs) 2)]}
  (api-request "/api/v1/compare"
    :params {:models (clojure.string/join "," model-slugs)}))
