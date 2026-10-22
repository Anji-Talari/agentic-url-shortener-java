# Three demonstrations

## 1. Greenfield
Requirement: New short-link creation and redirect service with analytics. Decomposition: understand → design/security (parallel-ready) → implementation → tests/documentation → approval → release readiness. Validation: call the actual URL API and run `mvn test`; workflow validation output is simulated. Risk: arbitrary link abuse; guard with validation and rate limiting in production.

## 2. Brownfield
Requirement: Existing URL service needs privacy-conscious analytics. Impacted modules: `UrlService`, `UrlController`, `urls/clicks` schema, tests, operations dashboards. Start `brownfield` workflow; advance; call `replan` with updated requirement (e.g., disallow tracking identifying user attributes) before approval. Observe task reset, workflow version increment, historical audit preservation; advance again. Risk: schema migrations and historic click correctness. Prototype makes no live DB migration.

## 3. Ambiguous
Requirement: “Make shortening secure and scalable.” Missing: SLA, traffic volume, auth, retention, threat model, latency budget. Start `ambiguous` workflow; advance; review agent assumptions; replan to `Only http(s), admin analytics, 99.9% target, max 100 RPS` **before** approval; advance; approve or reject. If rejected, workflow is terminal and a new workflow is required. Risk: team accepting undefined scope; human gate makes ambiguity visible.
