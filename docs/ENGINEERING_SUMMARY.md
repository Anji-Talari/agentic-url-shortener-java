# Engineering decision record

**Plan:** deliver both a functional shortener and a control-plane DAG engine. The shortener implements URL creation, redirects, expiry, deactivate, click analytics. The orchestrator demonstrates cross-stage dependencies, approval, durable audit, re-plan, failure control and manual retries.

**Why Java/Spring Boot:** representative enterprise stack, testable components, declarative transactions, JDBC persistence. H2 minimizes setup but is not a production database.

**Validation:** `mvn clean test` runs Spring/JUnit integration tests. Manual curl examples validate response shapes and approval transitions. These commands must be run in a machine with Maven and dependencies; execution in the authoring environment was not available.

**Trade-offs / limitations:** no LLM vendor or tool-calling agent integration; deterministic simulated agent outputs, not generated deployable application source. No background workers, queue, backpressure, distributed locks, OpenTelemetry, MTTR computation, robust API auth/RBAC, rate limiting, host/IP SSRF blocking, malware/phishing screening or real CI/CD rollback. URL shortener uses a simple random code and hit counter; analytics contains aggregate click totals only. Retry path is manually initiated after failure injection and bounded to two attempts. Approval is admin-key guarded but is not identity-backed approval. Single JVM design.

**Suggested next steps:** integrate model/tool adapters with typed outputs, sandboxed code edits, ephemeral CI verification, SHA artifact provenance; use PostgreSQL and atomic task leasing; collect per-task spans and MTTR; add role-based approvals, idempotency keys, abuse detection, SSRF protections and deployment with real rollback.

**Safety:** no production deployment occurs; high-impact actions remain blocked without explicit human decision. Audit history is retained on re-planning.
