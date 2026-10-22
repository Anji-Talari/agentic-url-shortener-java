# Agentic Software Engineering System — Java / Spring Boot

**Interview assignment:** URL Shortener + controlled, stateful SDLC orchestration. This is a self-contained Java 17, Spring Boot 3.3.5, Maven, H2 prototype.

## Setup

1. Install **JDK 17+** and **Maven 3.9+**.
2. Extract this ZIP, open the folder as a Maven project in IntelliJ IDEA.
3. Set an admin key (do not use the default outside your own laptop):

   Linux/macOS: `export ADMIN_API_KEY='replace-with-secret'`
   Windows PowerShell: `$env:ADMIN_API_KEY='replace-with-secret'`

4. Run `mvn clean test` followed by `mvn spring-boot:run`.
5. Service: `http://localhost:8080`; health: `GET /actuator/health`. Embedded **file-backed H2** database is created at `./data/agentic.mv.db`.

## URL shortener API

```bash
curl -X POST http://localhost:8080/api/urls -H 'Content-Type: application/json' -d '{"url":"https://example.com/docs","alias":"sample42"}'
curl -i http://localhost:8080/r/sample42
curl http://localhost:8080/api/urls/sample42/analytics -H 'X-Admin-Key: replace-with-secret'
```

`POST /api/urls` accepts `url`, optional `alias`, optional ISO-8601 `expiresAt`. `GET /r/{code}` returns HTTP 302. `GET /api/urls/{code}/analytics` returns click count; `DELETE /api/urls/{code}` deactivates. Analytics and deletion need `X-Admin-Key`.

## Agentic orchestration API

```bash
KEY='replace-with-secret'
curl -X POST http://localhost:8080/api/workflows -H "X-Admin-Key: $KEY" -H 'Content-Type: application/json' -d '{"scenario":"greenfield","requirement":"Create a short URL and click analytics"}'
# Copy the ID from the response and replace $ID:
curl -X POST "http://localhost:8080/api/workflows/$ID/advance" -H "X-Admin-Key: $KEY"
curl -X POST "http://localhost:8080/api/workflows/$ID/approval" -H "X-Admin-Key: $KEY" -H 'Content-Type: application/json' -d '{"actor":"engineering-lead","reason":"Reviewed test evidence","approved":true}'
curl -X POST "http://localhost:8080/api/workflows/$ID/advance" -H "X-Admin-Key: $KEY"
curl "http://localhost:8080/api/workflows/$ID" -H "X-Admin-Key: $KEY"
```

Other endpoints: `POST /{id}/replan` with `{"requirement":"..."}`, `POST /{id}/tasks/{task}/fail`, `POST /{id}/tasks/{task}/retry`, `POST /{id}/rollback`, `GET /metrics` (all prefixed `/api/workflows`, admin-protected). `advance` processes all currently-ready DAG nodes in topological waves in one transaction. Only a reviewer can resume the workflow after approval.

## Three required scenarios

- **Greenfield:** requirement `Build URL creation, redirects, expiry and click analytics`; advance → approval → advance → DONE.
- **Brownfield:** requirement `Add privacy-aware click analytics to an existing URL API`; advance, then replan with changed analytics constraints; outputs reset, prior audit retained; advance → approval → advance.
- **Ambiguous:** requirement `Make the shortener safe and fast`; security/design task captures assumptions; advance → reject until acceptance criteria clarified; instead replan **before rejecting** to refine requirements and run again through approval.

Read [architecture](docs/ARCHITECTURE.md), [scenario notes](docs/SCENARIOS.md), and [engineering review](docs/ENGINEERING_SUMMARY.md).

### Important prototype boundaries

The orchestration workers are **deterministic software agents**, not connected to Claude/Copilot/OpenAI. Task output represents a reviewable plan/status, not autonomous code generation, a real CI build, or external deployment. Failure injection/retries and compensating rollback are demonstrated as state transitions, not distributed artifact restoration. The sample application itself has automated Spring/JUnit tests, but validation task outputs in the workflow are simulations. Do not represent these as live AI actions or real production sign-off.
