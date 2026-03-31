# BenchGecko Clojure SDK

Official Clojure client for the [BenchGecko](https://benchgecko.ai) API. Query AI model data, benchmark scores, and run side-by-side comparisons from Clojure applications.

BenchGecko tracks every major AI model, benchmark, and provider. This library wraps the public REST API with idiomatic Clojure patterns using clj-http and Cheshire for JSON parsing.

## Installation

Add to your `project.clj`:

```clojure
[benchgecko "0.1.0"]
```

Or `deps.edn`:

```clojure
{benchgecko/benchgecko {:mvn/version "0.1.0"}}
```

## Quick Start

```clojure
(require '[benchgecko.core :as bg])

;; List all tracked AI models
(def models (bg/models))
(println (str "Tracking " (count models) " models"))

;; List all benchmarks
(def benchmarks (bg/benchmarks))
(doseq [b (take 5 benchmarks)]
  (println (:name b)))

;; Compare two models head-to-head
(def comparison (bg/compare ["gpt-4o" "claude-opus-4"]))
(doseq [m (:models comparison)]
  (println (:name m) (:scores m)))
```

## API Reference

### `(bg/models)`

Fetch all AI models tracked by BenchGecko. Returns a sequence of maps, each containing model metadata like name, provider, parameter count, pricing, and benchmark scores.

### `(bg/benchmarks)`

Fetch all benchmarks tracked by BenchGecko. Returns a sequence of maps with benchmark name, category, and description.

### `(bg/compare model-slugs)`

Compare two or more models side by side. Pass a vector of model slugs (minimum 2). Returns a map with `:models` key containing per-model comparison data.

## Configuration

Override the base URL and timeout using dynamic bindings:

```clojure
(binding [bg/*base-url* "http://localhost:3000"
          bg/*timeout* 10000]
  (bg/models))
```

## Error Handling

API errors throw `ExceptionInfo` with status code and response body:

```clojure
(try
  (bg/models)
  (catch Exception e
    (let [data (ex-data e)]
      (println "API error" (:status data) (:body data)))))
```

## Data Attribution

Data provided by [BenchGecko](https://benchgecko.ai). Model benchmark scores are sourced from official evaluation suites. Pricing data is updated daily from provider APIs.

## Links

- [BenchGecko](https://benchgecko.ai) - AI model benchmarks, pricing, and rankings
- [API Documentation](https://benchgecko.ai/api-docs)
- [GitHub Repository](https://github.com/BenchGecko/benchgecko-clojure)

## License

MIT
