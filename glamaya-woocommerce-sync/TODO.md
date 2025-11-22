# Glamaya WooCommerce Sync – Engineering TODO

This document consolidates code review findings into logically grouped, prioritized action items. Priorities: P1 (critical / correctness / high ROI), P2 (stability & reliability), P3 (structure & performance), P4 (future evolution / strategic).

---
## P1 – Critical Configuration & Blocking/Reliability Fixes
1. Kafka Producer Batch Size (DONE)
   - Changed `spring.kafka.producer.batch-size: 32KB` to numeric bytes `32768`.
   - Confirm property is picked up (log effective config at startup).
2. Reactive Blocking
   - Remove/contain `.block()` calls: `WebClient` fetch, `repository.save(...).block()`, Kafka send paths.
   - Short-term: isolate blocking in bounded scheduler / executor.
   - Medium-term: make fetch/persist/send an end-to-end reactive chain (`Mono`/`Flux`).
3. Fire-and-Forget Kafka Sends
   - Add delivery guarantees: configure `acks=all`, `retries`, optional `delivery.timeout.ms`.
   - Introduce error handling path (retry operator or DLQ topic: `woo-*-events-dlq`).
4. Null Query Params in OAuth Signature
   - Filter out null/blank params before building base string to avoid `key=` entries.
5. Exception Handling in `publishData`
   - Wrap WebClient calls with try/catch or convert to non-blocking `subscribe(onError=...)`.
   - Ensure non-2xx responses logged with context (entity/page/account).
6. Data Integrity – Modified Date Null Guards
   - Explicitly guard `getDateModifiedGmt` (Products / Orders). Fallback: created date or now.
7. Avoid Double Memory Usage on Fetch
   - Replace body-to-`String` then parse with streaming: `bodyToFlux(Entity.class).collectList()` or `bodyToMono(new ParameterizedTypeReference<List<Entity>>() {})`.
8. Polling Rate Control (Prevent Throttling)
   - Current active intervals (500–1000 ms) aggressive; add adaptive backoff (e.g. exponential on consecutive empty pages).

---
## P2 – Observability, Resilience & Consistency
1. Metrics (Micrometer)
   - Counters: `woo_sync_entities_processed_total{entity=}`, `woo_sync_entities_failed_total{entity=}`, `woo_sync_empty_page_total{entity=}`.
   - Timer: `woo_sync_batch_duration_seconds{entity=}`.
   - Gauge (optional): last successful page per entity.
2. Structured Logging
   - Standard key set: `entity= page= pageSize= account= items= durationMs= status=`.
3. Circuit Breaker / Fallback
   - If Mongo/Kafka repeatedly fail, pause syncing for entity (e.g., simple failure counter threshold -> passive mode duration).
4. Retry / Backoff for Transient HTTP Errors
   - Use `retryWhen(ExponentialBackoffSpec)` with max attempts + jitter.
5. Summary of Batch Outcomes
   - Add aggregate log/metric: processed vs failed per batch.
6. Coordinated Emission (Optional)
   - Wrap raw+derived Contact events if stronger ordering/atomicity needed (future outbox).

---
## P3 – Code Structure & Performance Hygiene
1. Abstract Common Processor Logic
   - Create `AbstractWooProcessor<T>`: template methods for fetch → persist → publish → optional n8n.
   - Strategy objects for endpoint, page size, mapping.
2. Remove Redundant Lombok / Comments
   - Drop unnecessary `@Getter` or commented headers in WebClient builder.
3. Optimize OAuth Signature Computation
   - Cache per-thread `Mac` instance (Mac not thread-safe) via `ThreadLocal<Mac>` to reduce object churn.
4. Consolidate Request Builder
   - Single method to build search request & query params, parameterizing entity-specific additions.
5. Deduplication Guard
   - Caffeine cache (recent IDs) to avoid re-emitting duplicates under paging anomalies.

---
## P4 – Strategic / Long-Term Enhancements
1. Event Schema Evolution
   - Consider Avro/Protobuf for forward/backward compatibility & schema registry.
2. Transactional Outbox Pattern
   - Persist events in Mongo, use Debezium or scheduled publisher for guaranteed delivery.
3. Multi-Tenant Scaling
   - Dynamic processor registration for multiple Woo accounts (`accountName` isolation, per-account config beans).
4. Full Reactive Refactor
   - Remove all imperative blocking; IntegrationFlow or pure Reactor pipeline with backpressure.
5. Advanced Rate Strategy
   - Rate-limit per entity using token bucket or leaky bucket; integrate API response headers if rate info available.

---
## Quick Wins Checklist (Execute First)
- [x] Fix Kafka `batch-size` to `32768`.
- [ ] Add null/blank query param filtering pre-signature.
- [ ] Implement metrics counters & timer.
- [ ] Guard modified dates (fallback rule defined).
- [ ] Stream HTTP deserialization (remove body-to-String). 
- [ ] Wrap `publishData` with robust error handling & structured logging.
- [ ] Introduce structured log format keys.

---
## Phase Plan
Phase 1 (Config + Observability)
- Batch size fix, metrics, structured logging, null param filtering.

Phase 2 (Reliability + Data Integrity)
- Modified date guards, publishData error handling, batch outcome summaries, retry/backoff HTTP, circuit breaker for persistent failures.

Phase 3 (Code Hygiene + Performance)
- Abstract base processor, streaming fetch, Mac caching, remove redundant annotations/comments, deduplication guard.

Phase 4 (Reactive & Delivery Guarantees)
- Full reactive pipeline, Kafka DLQ, stronger event coordination.

Phase 5 (Strategic Evolution)
- Schema migration (Avro/Protobuf), outbox pattern, multi-tenant scaling, advanced rate limiter.

---
## Implementation Sketches
1. Null Param Filtering
```java
Map<String,String> filtered = original.entrySet().stream()
  .filter(e -> e.getValue() != null && !e.getValue().isBlank())
  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
```
2. Reactive Fetch Example
```java
return webClient.get().uri(uriBuilder -> /* build */)
  .retrieve()
  .bodyToFlux(Product.class)
  .collectList(); // Mono<List<Product>>
```
3. Abstract Processor Outline
```java
public abstract class AbstractWooProcessor<T> {
  protected abstract String entity();
  protected abstract Mono<List<T>> fetchPage(int page);
  protected abstract Mono<Void> persist(List<T> batch);
  protected abstract Flux<SenderRecord<String, Object, String>> toEvents(List<T> batch);

  public Mono<Void> process(int page) {
    long start = System.nanoTime();
    return fetchPage(page)
      .flatMapMany(Flux::fromIterable)
      .collectList()
      .flatMap(list -> persist(list).thenMany(toEvents(list)).then())
      .doOnSuccess(v -> metrics.recordSuccess(entity(), page, start))
      .doOnError(err -> metrics.recordFailure(entity(), page, err));
  }
}
```
4. Metrics Naming
- `woo_sync_entities_processed_total{entity=}`
- `woo_sync_entities_failed_total{entity=}`
- `woo_sync_empty_page_total{entity=}`
- `woo_sync_batch_duration_seconds{entity=}`

---
## Risk & Mitigation Snapshot
| Risk | Impact | Mitigation |
| ---- | ------ | ---------- |
| Misparsed batch-size | Lower throughput / fallback defaults | Set numeric value & log effective config |
| Blocking IO | Thread starvation under load | Reactive chain + bounded schedulers |
| Silent Kafka failures | Data loss / inconsistency | Acks, retry, DLQ, outbox |
| API throttle | Sync halts / bans | Adaptive backoff & rate limiter |
| Missing date fields | NPE / incorrect lastUpdated | Null guard fallback |
| Unstructured logs | Hard diagnosis | Structured logging + metrics |

---
## Verification Targets
- After Phase 1: Metrics visible, correct batch-size logged, no null params in signed requests.
- After Phase 2: Transient HTTP failures retried, circuit breaker pauses after threshold.
- After Phase 3: Reduced duplicate code, no body-to-String conversions.
- After Phase 4: Zero `.block()` except at application boundary (if any). Kafka DLQ active.
- After Phase 5: Schema registry in place, multi-account support validated.

---
## Notes
Address all P1 items before increasing polling aggressiveness or enabling additional entities in production.
