(ns benchgecko.core
  "Clojure SDK for BenchGecko — compare LLM benchmarks, estimate inference
   costs, and explore AI model performance data across providers.

   Models are represented as maps with the following keys:
     :name           - Model identifier (e.g., \"gpt-4o\")
     :provider       - Provider name (e.g., \"OpenAI\")
     :context-window - Maximum context in tokens (optional)
     :scores         - Map of benchmark category keywords to scores (0-100)
     :pricing        - Map with :input-per-mtok and :output-per-mtok in USD (optional)

   Benchmark categories:
     :reasoning :coding :knowledge :instruction :multilingual
     :safety :long-context :vision :agentic")

(def ^:const benchmark-categories
  "All benchmark categories tracked by BenchGecko."
  #{:reasoning :coding :knowledge :instruction :multilingual
    :safety :long-context :vision :agentic})

(def ^:private tier-thresholds
  "Score thresholds for tier classification."
  [[90.0 :S]
   [80.0 :A]
   [70.0 :B]
   [60.0 :C]
   [0.0  :D]])

(defn make-model
  "Create a model map with required name and provider, plus optional fields.

   (make-model \"gpt-4o\" \"OpenAI\"
     :context-window 128000
     :scores {:reasoning 92.3 :coding 89.1}
     :pricing {:input-per-mtok 2.50 :output-per-mtok 10.00})"
  [name provider & {:keys [context-window scores pricing]}]
  (cond-> {:name name
           :provider provider
           :scores (or scores {})}
    context-window (assoc :context-window context-window)
    pricing        (assoc :pricing pricing)))

(defn average-score
  "Calculate the average benchmark score for a model.
   Returns nil if the model has no scores."
  [model]
  (let [scores (vals (:scores model))]
    (when (seq scores)
      (/ (reduce + scores) (count scores)))))

(defn model-tier
  "Classify a model into a performance tier (:S :A :B :C :D).
   Returns nil if the model has no scores.

   | Tier | Average Score | Description          |
   |------|---------------|----------------------|
   | :S   | 90+           | Elite frontier       |
   | :A   | 80-89         | Strong general       |
   | :B   | 70-79         | Capable mid-range    |
   | :C   | 60-69         | Budget / older gen   |
   | :D   | <60           | Entry-level / legacy |"
  [model]
  (when-let [avg (average-score model)]
    (some (fn [[threshold tier]]
            (when (>= avg threshold) tier))
          tier-thresholds)))

(defn value-score
  "Compute value score: average benchmark performance per dollar of blended
   token price. Higher is better. Returns nil if pricing or scores are missing."
  [model]
  (when-let [avg (average-score model)]
    (when-let [pricing (:pricing model)]
      (let [blended (+ (:input-per-mtok pricing) (:output-per-mtok pricing))]
        (when (pos? blended)
          (/ avg blended))))))

(defn get-model
  "Look up a model by name from a collection of models.
   Returns the first model matching the given name, or nil."
  [models name]
  (first (filter #(= (:name %) name) models)))

(defn compare-models
  "Compare two models across all mutually-scored benchmark categories.
   Returns a map with:
     :model-a  - First model
     :model-b  - Second model
     :deltas   - Map of category to score difference (positive = A leads)
     :a-wins   - Categories where model A scores higher
     :b-wins   - Categories where model B scores higher
     :ties     - Categories with identical scores
     :winner   - The model with higher average score (ties favor A)

   (compare-models gpt4 claude)"
  [model-a model-b]
  (let [scores-a (:scores model-a)
        scores-b (:scores model-b)
        shared   (filter #(and (contains? scores-a %) (contains? scores-b %))
                         benchmark-categories)
        deltas   (into {} (map (fn [cat]
                                 [cat (- (get scores-a cat) (get scores-b cat))])
                               shared))
        grouped  (group-by (fn [[_ delta]]
                             (cond
                               (< (abs delta) 0.001) :ties
                               (pos? delta)          :a-wins
                               :else                 :b-wins))
                           deltas)
        extract  (fn [k] (mapv first (get grouped k [])))]
    {:model-a model-a
     :model-b model-b
     :deltas  deltas
     :a-wins  (extract :a-wins)
     :b-wins  (extract :b-wins)
     :ties    (extract :ties)
     :winner  (let [avg-a (or (average-score model-a) 0)
                    avg-b (or (average-score model-b) 0)]
                (if (> avg-b avg-a) model-b model-a))}))

(defn estimate-cost
  "Estimate inference cost in USD for a given number of input and output tokens.
   Returns nil if the model has no pricing information.

   (estimate-cost gpt4 5000 2000)  ;=> 0.0325"
  [model input-tokens output-tokens]
  (when-let [pricing (:pricing model)]
    (+ (* (/ input-tokens 1000000.0) (:input-per-mtok pricing))
       (* (/ output-tokens 1000000.0) (:output-per-mtok pricing)))))

(defn estimate-monthly
  "Estimate monthly cost assuming a daily request volume.

   (estimate-monthly gpt4 1000 3000 1000)  ;=> monthly USD cost"
  [model daily-requests avg-input-tokens avg-output-tokens]
  (when-let [per-request (estimate-cost model avg-input-tokens avg-output-tokens)]
    (* per-request daily-requests 30)))

(defn rank-by-category
  "Rank models by score in a specific benchmark category (descending).
   Models without a score in that category are excluded.

   (rank-by-category models :coding)
   ;=> [{:model gpt4 :score 89.1} {:model claude :score 93.7}]"
  [models category]
  (->> models
       (filter #(contains? (:scores %) category))
       (map (fn [m] {:model m :score (get (:scores m) category)}))
       (sort-by :score >)))

(defn filter-by-tier
  "Filter models by performance tier.

   (filter-by-tier models :S)  ;=> sequence of S-tier models"
  [models tier]
  (filter #(= (model-tier %) tier) models))

(defn best-value
  "Find the model with the best value score (performance per dollar).
   Returns nil if no models have both pricing and scores."
  [models]
  (->> models
       (filter value-score)
       (sort-by value-score >)
       first))

(defn model-summary
  "Generate a human-readable summary string for a model.

   (model-summary gpt4)
   ;=> \"gpt-4o (OpenAI) [S-Tier] avg=90.0 value=7.2\""
  [model]
  (let [tier-str (if-let [t (model-tier model)]
                   (str " [" (name t) "-Tier]")
                   "")
        avg-str  (if-let [a (average-score model)]
                   (format " avg=%.1f" (double a))
                   "")
        val-str  (if-let [v (value-score model)]
                   (format " value=%.1f" (double v))
                   "")]
    (str (:name model) " (" (:provider model) ")" tier-str avg-str val-str)))
