# Existing Document Content

---

## Newly Identified Improvements (Codebase Scan)

P1

- Validate YAML configuration to eliminate duplicate keys (ensure single `kafka:` root) and add startup assert.
- Add null-safe guards when converting searchRequest to Map (avoid NPE if ObjectMapper returns null).
- Introduce central error classification enum (TRANSIENT, PERMANENT, MAPPING, DOWNSTREAM) used in DLQ headers.

P2

- WebClient: add connection / read timeout, retry for idempotent GET, configure compression, and limit max header size.
- Metrics: add slow Kafka send counter and webhook failure counter; percentiles for fetch duration already present – add p95/p99 export.
- Logging: adopt single logger marker `woo_sync` and key=value pattern via helper.
- Add health endpoint exposing tracker states (page, lastUpdated, backoff).

P3

- Replace reflection date extraction with marker interface implementation in generated Woo POJOs (jsonschema2pojo custom annotator).
- Consolidate WebClient beans: move shared builder logic (timeouts, codecs) to helper to prevent drift.
- KafkaProducer: extract DLQ resolution strategy into dedicated class for clarity & unit test.
- EventPublisher: expose asynchronous result or reactive Mono for composing send outcomes.
- AbstractWooProcessor: extract fetch pipeline into smaller private methods (buildQueryParams, executeFetch, handleResponse) for readability.
- StatusTracker: add optimistic locking (version field) to prevent race conditions under future concurrency.
- Poller: externalize backoff parameters (max exponent, jitter range) into config properties.

P4

- Introduce circuit breaker/resilience layer (Resilience4j) around WooCommerceClient + Kafka send.
- Support graceful shutdown: drain resultsQueue, flush Kafka, stop polling trigger.
- Multi-tenant readiness: pass account-specific credentials via registry; remove direct @Value injection from KafkaProducer for sourceAccountName.

Security / Compliance

- Sign outgoing webhook payloads with HMAC header (shared secret) for n8n/webhooks.
- Mask sensitive values (consumer key/secret) in debug logs; ensure no accidental exposure.
- Add dependency updates audit (OWASP dependency check plugin) and generate report.

Performance

- Configure Reactor Netty connection pool: max connections, pending acquire timeout.
- Instrument per-item processing time (Timer) separate from fetch duration.
- Use `publishOn(Schedulers.boundedElastic())` only where blocking call exists; audit unnecessary boundedElastic usage.

Testing

- Add unit tests for DLQ routing logic & error classification.
- Integration test for adaptive backoff (simulate sequence of empty/non-empty responses).
- Contract tests for mapping Woo entities to Contact via existing mapper factory.

Tooling / DX

- Add README section with troubleshooting (Kafka lag, backoff diagnostics).
- Provide sample dashboard JSON (Grafana) for metrics names.
- Include Makefile or script shortcuts (build, run, lint).

Documentation

- Javadoc on each processor hook clarifying invariants (id uniqueness, formatting side effects).
- Architecture diagram update reflecting inlined builders and ports.

Next Action Recommendation

1. Implement metrics & structured logging helper.
2. Introduce error classification + DLQ header enrichment.
3. Replace reflection date extraction (update POJO generation annotator).
4. Add circuit breaker + basic retry for Woo GET (429/5xx).
5. Add tracker health endpoint & tests.
