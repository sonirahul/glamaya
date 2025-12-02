# Glamaya Sync Service - Architectural Document (Reactive Edition)

## 1. Vision & Goals

The **`glamaya-sync`** service consolidates multiple platform-specific sync microservices (e.g.
`glamaya-woocommerce-sync`, `glamaya-wix-sync`) into a single extensible **modular monolith (modulith)** while
preserving clear boundaries and enabling future platform onboarding with minimal effort.

### Key Objectives

- **Reduce Duplication:** Centralize orchestration, status tracking, notification fan-out, and generic concerns.
- **Increase Extensibility:** New platforms (Shopify, Magento, etc.) plug in via dedicated `platform-*` modules.
- **Reactive, Non-Blocking:** End-to-end reactive pipeline (Reactor: Mono/Flux) for scalable IO (HTTP APIs, Kafka, DB).
- **Testability & Maintainability:** Hexagonal architecture keeps business logic framework-independent.
- **Resilience & Observability:** Standard patterns (circuit breakers, backpressure, structured logging) applied
  consistently.
- **Incremental Migration:** Greenfield build allows zero-downtime cutover from legacy services.

---

## 2. Architectural Pattern: Hexagonal (Ports & Adapters)

Core (hexagon) contains pure domain + application orchestration logic and exposes **ports**. External technology
concerns live in **adapters** provided by platform modules and the runner.

Port Types:

- **Inbound (Driving):** `SyncPlatformUseCase` – triggers synchronization.
- **Outbound (Driven):** `DataProvider`, `DataMapper`, `StatusStorePort`, `NotificationPort`, `SyncProcessor`,
  `ProcessorConfiguration`.

Adapters:

- **Primary (Driving):** Scheduler, REST controllers (future), message triggers.
- **Secondary (Driven):** API clients, persistence, notification channels (Kafka, webhooks), platform-specific
  configuration loading.

Core depends only on abstractions. Runner / platform modules implement and wire concrete adapters (Spring, HTTP clients,
Kafka, DB, etc.).

---

## 3. Module Structure

```plaintext
glamaya-sync/
├── pom.xml                  (Parent POM / BOM)
├── core/                    (Domain + application services + ports)
├── platform-woocommerce/    (WooCommerce adapters)
├── platform-wix/            (Future: Wix adapters)
└── runner/                  (Spring Boot assembly + scheduling + composite notifications)
```

### 3.1. `core`

- Reactive domain orchestration (`SyncOrchestrationService`).
- Domain models (`ProcessorStatus`, `ProcessorType`, `SyncContext`).
- Port interfaces (reactive signatures).
- No framework annotations.

### 3.2. `platform-*`

- Platform-specific implementations of ports.
- Pagination/cursor logic remains here (e.g. WooCommerce pages vs Wix cursors).
- Configuration binding (YAML → typed config via Spring for convenience).

### 3.3. `runner`

- Spring Boot entrypoint and DI wiring.
- Reactive Kafka producer config.
- Composite notification adapter.
- Scheduler driving periodic sync.

---

## 4. Package Layout (Core)

```plaintext
com.glamaya.sync.core/
├── application/
│   ├── service/      // SyncOrchestrationService
│   └── usecase/      // SyncPlatformUseCase
├── domain/
│   ├── model/        // ProcessorStatus, ProcessorType, SyncContext, NotificationType
│   └── port/
│       └── out/      // Ports (DataProvider, DataMapper, StatusStorePort, NotificationPort, etc.)
```

---

## 5. Reactive Port Interfaces & Domain Models

### 5.1. `PlatformAdapter`

```java
public interface PlatformAdapter {
    String getPlatformName();
    Mono<Void> sync();
}
```

### 5.2. `DataProvider<T>` (Platform owns pagination/cursor)

```java
public interface DataProvider<T> {
    Flux<T> fetchData(SyncContext<?> context);
}
```

Each platform implementation is responsible for emitting a Flux spanning all necessary pages/cursor windows for a single
run.

### 5.3. `DataMapper<P, C>`

```java
public interface DataMapper<P, C> {
    C mapToCanonical(P platformModel);
}
```

### 5.4. `StatusStorePort`

```java
public interface StatusStorePort {
    Mono<ProcessorStatus> findStatus(ProcessorType processorType);
    Mono<Void> saveStatus(ProcessorStatus status);
}
```

### 5.5. `NotificationPort<C>` (Channel-aware)

```java
public interface NotificationPort<C> {
    boolean supports(NotificationType type);
    Mono<Void> notify(C payload, ProcessorConfiguration<?> processorConfiguration, NotificationType type);
}
```

- Each leaf adapter (Kafka, webhook, N8N, etc.) declares supported channels.
- Composite adapter fans out per enabled channel.

### 5.6. `ProcessorConfiguration<T>` & NotificationConfig

```java
public interface ProcessorConfiguration<T> {
    T get();
    boolean isEnable();
    boolean isResetOnStartup();
    Integer getInitPage();
    Integer getPageSize();
    String getQueryUrl();
    NotificationConfig getNotificationConfig(NotificationType notificationType);
    interface NotificationConfig {
        Boolean getEnable();
        String getTopic();
        String getWebhook();
    }
}
```

Platform implementations (e.g. `APIConfig`) maintain `EnumMap<NotificationType, NotificationConfig>` for strong typing.

### 5.7. `SyncProcessor<P, C, T>`

Aggregates DataProvider, DataMapper, processor type and typed configuration.

```java
public interface SyncProcessor<P, C, T> {
    DataProvider<P> getDataProvider();
    DataMapper<P, C> getDataMapper();
    ProcessorType getProcessorType();
    ProcessorConfiguration<T> getConfiguration();
}
```

### 5.8. Domain Models

```java
public enum NotificationType { KAFKA, N8N }
public enum ProcessorType { WOOCOMMERCE_ORDER /*, etc.*/ }
public record SyncContext<T>(ProcessorStatus status, ProcessorConfiguration<T> configuration) {}
```

`ProcessorStatus` tracks evolving sync state (cursor/nextPage/etc.) managed by platform adapters.

---

## 6. Reactive Orchestration Flow (Core)

High-level pipeline (current implementation):

1. Load or initialize `ProcessorStatus` from `StatusStorePort`.
2. Build `SyncContext`.
3. Invoke `DataProvider.fetchData(syncContext)` – platform handles paging/cursor.
4. Map each platform item → canonical using `DataMapper`.
5. Determine enabled notification channels once from `ProcessorConfiguration`.
6. For each canonical item, notify across enabled channels (bounded concurrency).
7. Update `ProcessorStatus` (e.g. `lastSuccessfulRun`, `totalItemsSynced`) and persist.

Backpressure & concurrency are managed via Reactor operators (e.g. `flatMap(..., concurrencyLimit)`).

---

## 7. Platform-Specific Pagination Strategy

- **WooCommerce:** Page-based (page number, page size). DataProvider iterates pages internally and emits a continuous
  Flux.
- **Wix (future):** Cursor-based (opaque token). DataProvider dereferences cursors until exhaustion.
- Core remains uninvited to pagination details; termination is indicated by Flux completion.

---

## 8. Composite Notification System (Reactive Fan-Out)

### 8.1. Leaf Adapters

Example: Kafka adapter

```java
@Component
public class KafkaNotificationAdapter implements NotificationPort<Object> {
    public boolean supports(NotificationType type) { return type == NotificationType.KAFKA; }
    public Mono<Void> notify(Object payload, ProcessorConfiguration<?> cfg, NotificationType type) {
        var nc = cfg.getNotificationConfig(type);
        if (nc == null || !Boolean.TRUE.equals(nc.getEnable()) || nc.getTopic() == null) return Mono.empty();
        return kafkaTemplate.send(nc.getTopic(), payload).then();
    }
}
```

### 8.2. Composite Adapter

```java
@Component("compositeNotificationAdapter")
@Primary
public class CompositeNotificationAdapter implements NotificationPort<Object> {
    private final List<NotificationPort<Object>> delegates;
    public boolean supports(NotificationType type) { return true; }
    public Mono<Void> notify(Object payload, ProcessorConfiguration<?> cfg, NotificationType type) {
        var channelCfg = cfg.getNotificationConfig(type);
        if (channelCfg == null || !Boolean.TRUE.equals(channelCfg.getEnable())) return Mono.empty();
        return Flux.fromIterable(delegates)
                   .filter(d -> d.supports(type))
                   .flatMap(d -> d.notify(payload, cfg, type))
                   .then();
    }
}
```

### 8.3. Configuration

```yaml
glamaya:
  sync:
    woocommerce:
      api:
        endpoint-configs:
          WOOCOMMERCE_ORDER:
            notifications:
              kafka:
                enable: true
                topic: woocommerce-orders
              n8n:
                enable: false
                webhook: https://n8n.example/webhook/order
```

### 8.4. Channel Enablement

Enabled channels are derived from `ProcessorConfiguration.getNotificationConfig(notificationType)`; absent or disabled
configs result in skipping.

---

## 9. Resilience & Error Handling (Planned)

- **Retries / Circuit Breaking:** Applied at platform adapter HTTP client layer (Resilience4j + Reactor).
- **Dead Letter / DLQ:** Kafka producer adapter handles DLQ topics on failures (future consolidation into runner).
- **Partial Failure Tolerance:** Notification failures logged and skipped without aborting entire sync.

---

## 10. Observability (Planned Improvements)

- Structured logging with processorType, notificationType, page/cursor.
- Micrometer metrics (items processed, notification count, failures, latency per channel).
- Tracing (OpenTelemetry) for multi-channel fan-out (future).

---

## 11. Extensibility Patterns

- **Add Processor to Existing Platform:** Implement new `SyncProcessor` bean (DataProvider + DataMapper +
  configuration). Add YAML endpoint config + notifications block.
- **Add Notification Channel:** Extend `NotificationType` enum, create leaf adapter implementing `NotificationPort`, add
  configuration block.
- **Add Platform:** New `platform-<name>` module with adapters, SyncProcessors, and PlatformAdapter orchestrating
  relevant ProcessorTypes.

---

## 12. Design Principles

| Principle             | Status     | Notes                                                                                  |
|-----------------------|------------|----------------------------------------------------------------------------------------|
| SRP                   | Good       | SyncOrchestrationService focuses on orchestration; platform adapters on API specifics. |
| OCP                   | Good       | New processors/channels via enum + config + adapter.                                   |
| DIP                   | Strong     | Core depends only on interfaces; no Spring types.                                      |
| DRY                   | Acceptable | Some duplication in copying config; centralization possible.                           |
| Reactive Non-Blocking | Achieved   | All IO-facing ports return Mono/Flux.                                                  |

---

## 13. Future Enhancements

- Pagination advancement reporting back to core (optional counters).
- Metrics & tracing integration.
- Unified error classification (transient vs permanent).
- Backpressure tuning per channel concurrency.
- Optional Spring Integration flows for visual orchestration (runner only).

---

## 14. Migration Phases (Summary)

1. Implement WooCommerce in modulith → cutover.
2. Add Wix module → cutover.
3. Onboard new platforms (repeatable pattern).

---

## 15. Glossary

| Term                 | Meaning                                                                       |
|----------------------|-------------------------------------------------------------------------------|
| Canonical Model      | Unified domain representation independent of platform.                        |
| SyncProcessor        | Aggregates DataProvider + DataMapper + configuration for one `ProcessorType`. |
| Notification Channel | Destination for canonical events (Kafka, webhook, etc.).                      |
| Modulith             | Modular monolith with enforced boundaries via modules.                        |

---

## 16. Revision Log

| Version | Date       | Author   | Notes                                                 |
|---------|------------|----------|-------------------------------------------------------|
| 1.0     | Initial    | -        | Original synchronous draft                            |
| 1.1     | 2025-12-02 | Refactor | Reactive port alignment + updated notification design |
