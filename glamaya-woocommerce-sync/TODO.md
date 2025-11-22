# Glamaya WooCommerce Sync – Engineering TODO

_Status Legend:_
- ~~Item~~ (DONE)
- Item (PARTIAL) indicates work started but not fully complete.
- Item (TODO) indicates not yet started.

---
## P1 – Critical Configuration & Blocking/Reliability Fixes
1. ~~Kafka Producer Batch Size~~ (DONE)
   - Changed `spring.kafka.producer.batch-size: 32KB` to numeric bytes `32768`.
   - Confirm property is picked up (log effective config at startup).
2. Reactive Blocking (PARTIAL)
   - Reduced blocking: fetch & persist now reactive; remaining `.block()` only at MessageSource boundary.
   - Next: migrate polling adapter to fully reactive to remove boundary `.block()`.
3. ~~Fire-and-Forget Kafka Sends~~ (DONE)
   - Added delivery guarantees (`acks=all`, `retries`, timeouts) & DLQ routing logic.
   - Future: add retry operator & structured success/failure metrics.
4. ~~Null Query Params in OAuth Signature~~ (DONE)
   - Filter applied before signature building in `OAuth1Service`.
5. ~~Exception Handling in `publishData`~~ (DONE)
   - Non-blocking `publishData` with contextual success / non-2xx / error logging.
6. Data Integrity – Modified Date Null Guards (TODO)
   - User processor has fallback; Product/Order still assume non-null `getDateModifiedGmt`.
   - To do: unify fallback strategy across all entities.
7. ~~Avoid Double Memory Usage on Fetch~~ (DONE)
   - Switched to streaming `bodyToFlux(elementClass).collectList()`.
8. Polling Rate Control (Prevent Throttling) (TODO)
   - Adaptive backoff not yet implemented.

---
## P2 – Observability, Resilience & Consistency
1. Metrics (Micrometer) (TODO)
2. Structured Logging (PARTIAL)
   - Context logging added for n8n + Kafka, but global key=value format not standardized yet.
3. Circuit Breaker / Fallback (TODO)
4. Retry / Backoff for Transient HTTP Errors (TODO)
5. Summary of Batch Outcomes (TODO)
6. Coordinated Emission (Optional) (TODO)

---
## P3 – Code Structure & Performance Hygiene
1. Abstract Common Processor Logic (TODO)
2. Remove Redundant Lombok / Comments (TODO)
3. Optimize OAuth Signature Computation (TODO)
4. Consolidate Request Builder (TODO)
5. Deduplication Guard (TODO)

---
## P4 – Strategic / Long-Term Enhancements
1. Event Schema Evolution (TODO)
2. Transactional Outbox Pattern (TODO)
3. Multi-Tenant Scaling (TODO)
4. Full Reactive Refactor (TODO)
5. Advanced Rate Strategy (TODO)

---
## Quick Wins Checklist (Execute First)
- [x] Fix Kafka `batch-size` to `32768`.
- [x] Add null/blank query param filtering pre-signature.
- [ ] Implement metrics counters & timer.
- [ ] Guard modified dates (fallback rule defined across all entities).
- [x] Stream HTTP deserialization (remove body-to-String).
- [x] Wrap `publishData` with robust error handling & contextual logging.
- [ ] Introduce standardized structured log format keys (page, pageSize, entity, account, items, status, durationMs).

---
## Phase Plan
_Phase 1 remaining_: metrics, structured logging standardization, modified date guards.

Phase 2: batch outcome metrics, retry/backoff, circuit breaker.

Phase 3: abstract processor, deduplication cache, Mac caching, request builder refactor.

Phase 4: fully reactive polling (remove boundary `.block()`), advanced DLQ + retry operator, coordinated emission.

Phase 5: schema evolution, outbox, multi-tenant, adaptive rate strategy.

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
_Update after each phase; current:_
- Batch size fixed.
- OAuth null filtering active.
- Kafka reliability + DLQ in place.
- Streaming fetch implemented.
- PublishData contextual logging active.

Pending: metrics, modified date unification, structured global logging.

---
## Notes
Address remaining Phase 1 items before increasing polling aggressiveness or enabling new entities.
