# benchgecko

Clojure SDK for [BenchGecko](https://benchgecko.ai) -- the data platform for comparing AI model benchmarks, estimating inference costs, and exploring performance across providers.

## Overview

`benchgecko` provides idiomatic Clojure functions for working with LLM benchmark data. Models are plain maps, operations are pure functions, and everything composes naturally. Build comparison tools, cost calculators, and leaderboard UIs with data-driven simplicity.

The library provides:

- **make-model** for constructing model maps with scores and pricing
- **compare-models** for head-to-head analysis across shared benchmark categories
- **estimate-cost** and **estimate-monthly** for inference cost calculations
- **model-tier** and **filter-by-tier** for S/A/B/C/D tier classification
- **rank-by-category** for leaderboard sorting across 9 benchmark dimensions
- **best-value** for finding the most cost-effective model
- **value-score** for computing performance-per-dollar ratios

Models are represented as plain Clojure maps with keyword keys, making them trivial to serialize, transform, and compose with the rest of your data pipeline.

## Installation

### Leiningen/Boot

```clojure
[org.clojars.dropthe/benchgecko "0.1.0"]
```

### deps.edn

```clojure
org.clojars.dropthe/benchgecko {:mvn/version "0.1.0"}
```

## Quick Start

```clojure
(require '[benchgecko.core :as bg])

;; Define models as data
(def gpt4
  (bg/make-model "gpt-4o" "OpenAI"
    :context-window 128000
    :scores {:reasoning 92.3 :coding 89.1 :knowledge 88.7}
    :pricing {:input-per-mtok 2.50 :output-per-mtok 10.00}))

(def claude
  (bg/make-model "claude-sonnet-4" "Anthropic"
    :context-window 200000
    :scores {:reasoning 94.1 :coding 93.7 :knowledge 91.2}
    :pricing {:input-per-mtok 3.00 :output-per-mtok 15.00}))

;; Compare across shared categories
(let [result (bg/compare-models gpt4 claude)]
  (println "Winner:" (:name (:winner result)))
  (println "Claude wins:" (:b-wins result))
  (println "GPT-4o wins:" (:a-wins result)))

;; Estimate cost for a request
(bg/estimate-cost gpt4 5000 2000)  ;=> 0.0325
```

## Tier Classification

Models are classified into performance tiers based on their average benchmark score. The classification is a pure function over the scores map:

| Tier | Average Score | Description |
|------|--------------|-------------|
| :S | 90+ | Elite frontier models |
| :A | 80-89 | Strong general-purpose models |
| :B | 70-79 | Capable mid-range models |
| :C | 60-69 | Budget or older generation |
| :D | <60 | Entry-level or legacy |

```clojure
(bg/model-tier gpt4)           ;=> :S
(bg/average-score gpt4)        ;=> 90.03

;; Filter a collection by tier
(bg/filter-by-tier models :S)  ;=> seq of S-tier models

;; Rank by a specific category
(bg/rank-by-category models :coding)
;=> [{:model claude :score 93.7} {:model gpt4 :score 89.1}]
```

## Cost Estimation

All cost functions are pure and return nil when pricing data is missing, making them safe to use in pipelines:

```clojure
;; Single request cost
(bg/estimate-cost gpt4 10000 5000)       ;=> 0.075

;; Monthly budget projection
(bg/estimate-monthly gpt4 1000 3000 1000) ;=> ~262.50

;; Value analysis (performance per dollar)
(bg/value-score gpt4)     ;=> 7.2
(bg/value-score claude)   ;=> 5.2
(bg/best-value [gpt4 claude budget-model])  ;=> most cost-effective model
```

## Data-Driven Design

Models are plain maps, so you can use all standard Clojure operations:

```clojure
;; Add a score to an existing model
(update gpt4 :scores assoc :safety 87.5)

;; Filter models with threading
(->> models
     (filter #(> (or (bg/average-score %) 0) 85))
     (sort-by bg/value-score >)
     (take 5))

;; Serialize to EDN or JSON trivially
(pr-str gpt4)
```

## Benchmark Categories

The library tracks 9 benchmark dimensions aligned with [BenchGecko](https://benchgecko.ai):

| Category | Keyword | Typical Benchmarks |
|----------|---------|-------------------|
| Reasoning | :reasoning | GSM8K, MATH, ARC |
| Coding | :coding | HumanEval, MBPP, SWE-bench |
| Knowledge | :knowledge | MMLU, HellaSwag |
| Instruction | :instruction | MT-Bench, AlpacaEval |
| Multilingual | :multilingual | MGSM, XLSum |
| Safety | :safety | TruthfulQA, BBQ |
| Long Context | :long-context | RULER, Needle-in-a-Haystack |
| Vision | :vision | MMMU, MathVista |
| Agentic | :agentic | WebArena, SWE-bench |

## Data Source

Benchmark data, model metadata, and pricing information are maintained by [BenchGecko](https://benchgecko.ai). Visit the platform for live leaderboards, interactive comparisons, and the full model database covering 300+ models across 50+ providers.

## License

MIT
