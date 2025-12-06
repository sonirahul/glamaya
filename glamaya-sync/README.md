# Glamaya Sync

A modular, reactive synchronization service that aggregates multiple e‑commerce / platform specific integrations (
WooCommerce, Wix, etc.) into a single extensible modulith. Built with Hexagonal Architecture (Ports & Adapters) and
Reactor for non‑blocking IO.

## Modules

| Module                 | Purpose                                                                                          |
|------------------------|--------------------------------------------------------------------------------------------------|
| `core`                 | Pure domain & orchestration logic (no Spring) – ports, models, services.                         |
| `platform-woocommerce` | WooCommerce specific adapters: data provider, mappers, configuration loaders.                    |
| `platform-wix`         | (Future) Wix adapters.                                                                           |
| `runner`               | Spring Boot assembly: wiring, scheduler, composite notifications, infrastructure (Kafka, Mongo). |

## Reactive Design

All outbound ports use Reactor types:

- Fetching: `Flux<T>` from `DataProvider` spans all pages/cursor windows for one run.
- Notifications: `Mono<Void>` per channel.
- Persistence: `Mono<ProcessorStatus>` / `Mono<Void>` for status tracking.

Platform modules own pagination/cursor mechanics. Core consumes a unified Flux and performs: fetch → map → notify.

## Key Ports (Interfaces)

```java
interface DataProvider<T> { Flux<T> fetchData(SyncContext<?> ctx); }
interface DataMapper<P,C> { C mapToCanonical(P platformModel); }
interface StatusStorePort { Mono<ProcessorStatus> findStatus(ProcessorType type); Mono<Void> saveStatus(ProcessorStatus s); }
interface NotificationPort<C> { boolean supports(NotificationType type); Mono<Void> notify(C payload, ProcessorConfiguration<?> cfg, NotificationType type); }
interface SyncProcessor<P,C,T> { DataProvider<P> getDataProvider(); DataMapper<P,C> getDataMapper(); ProcessorType getProcessorType(); ProcessorConfiguration<T> getConfiguration(); }
```

## Configuration (Example – WooCommerce)

```yaml
glamaya:
  sync:
    woocommerce:
      api:
        endpoint-configs:
          WOOCOMMERCE_ORDER:
            enable: true
            init-page: 1
            page-size: 50
            query-url: /wp-json/wc/v3/orders
            notifications:
              kafka:
                enable: true
                topic: woocommerce-orders
              n8n:
                enable: false
                webhook: https://n8n.example/webhook/order
```

## Adding a New Processor to an Existing Platform

Follow these steps to add (e.g.) `WOOCOMMERCE_PRODUCT`:

1. Add enum value to `ProcessorType`:
   ```java
   public enum ProcessorType { WOOCOMMERCE_ORDER, WOOCOMMERCE_PRODUCT }
   ```
2. Extend platform configuration YAML under `endpoint-configs` with `WOOCOMMERCE_PRODUCT` block (similar to ORDER),
   including a `notifications` section if needed.
3. Implement `DataProvider<ProductDto>` – handle paging/cursor specifics; emit `Flux<ProductDto>`.
4. Implement `DataMapper<ProductDto, GlamayaProduct>` converting platform DTO → canonical model.
5. Create a `SyncProcessor<ProductDto, GlamayaProduct, APIConfig>` bean wiring the provider, mapper, and configuration (
   e.g. in a Spring `@Configuration` class inside the platform module).
6. (Optional) Add notification channel specific logic if product events differ (usually reuses existing
   `NotificationPort` leaf adapters).
7. Deploy & observe logs/metrics to confirm items are processed and published.

## Adding a New Platform Module (e.g. Shopify)

1. Create new Maven module: `platform-shopify/` depending on `core`.
2. Define platform-specific configuration class (similar to `APIConfig`) implementing `ProcessorConfiguration`.
3. Implement each required `DataProvider` and `DataMapper` pairing for the initial set of `ProcessorType` values (add
   new enum entries).
4. Provide a `PlatformAdapter` (e.g. `ShopifyPlatformAdapter`) that calls `syncOrchestrator.sync(<ProcessorType>)`
   for each supported processor in sequence.
5. Add YAML file: `application-shopify.yml` with `glamaya.sync.shopify.api.endpoint-configs` structure and notification
   blocks.
6. Register beans via Spring `@Configuration` in the module for processors and the platform adapter.
7. Include the new module dependency in `runner/pom.xml` and add its package to
   `@SpringBootApplication(scanBasePackages=...)` if not already covered.
8. Add scheduler enablement (existing `SyncScheduler` will pick up new `PlatformAdapter` automatically if it is a Spring
   bean).
9. Validate sync in staging (compare counts, sample payloads) and cut over.

## Adding a New Notification Channel

1. Add enum value to `NotificationType` (e.g. `WEBHOOK`).
2. Extend platform YAML notification section with that channel’s configuration keys (`enable`, `topic` or `webhook`).
3. Implement a leaf adapter (`WebhookNotificationAdapter`) in `runner`:
   ```java
   @Component
   public class WebhookNotificationAdapter implements NotificationPort<Object> {
       public boolean supports(NotificationType t) { return t == NotificationType.WEBHOOK; }
       public Mono<Void> notify(Object payload, ProcessorConfiguration<?> cfg, NotificationType t) {
           var nc = cfg.getNotificationConfig(t);
           if (nc == null || !Boolean.TRUE.equals(nc.getEnable()) || nc.getWebhook() == null) return Mono.empty();
           return webClient.post().uri(nc.getWebhook()).bodyValue(payload).retrieve().bodyToMono(Void.class);
       }
   }
   ```
4. Composite adapter automatically picks it up (ensure bean is in context).

## Running Locally

Prerequisites: JDK 21+, Docker (for Kafka/Mongo), Maven.

### 1. Start Infra (example docker-compose pointer)

```bash
docker compose -f ci-cd/docker/docker-compose.yml up -d kafka mongo
```

### 2. Build

```bash
mvn -q clean package
```

### 3. Run (local profile)

```bash
java -jar runner/target/runner-*.jar --spring.profiles.active=local
```

### 4. Logs & Verification

- Observe startup logs for registered `PlatformAdapter`s and `SyncProcessor`s.
- Confirm Kafka topic receives messages (use `kafka-console-consumer`).

## Testing Strategy (Recommended)

| Test Type   | Scope                                       | Examples                                                 |
|-------------|---------------------------------------------|----------------------------------------------------------|
| Unit        | Port implementations & mappers              | DataMapper correctness, Notification adapter skip logic. |
| Integration | Processor end-to-end with mock DataProvider | Reactor pipeline counts items, persists status.          |
| Contract    | Platform API DTOs                           | Verify serialization/parsing of WooCommerce responses.   |
| Load        | Large page/cursor streams                   | Backpressure behavior and notification latency.          |

## Observability & Metrics (Upcoming)

- Micrometer counters: `sync.items.processed`, `sync.notifications.sent{channel}`, `sync.errors{stage}`.
- Timers: duration per processor run.
- Tracing: spans for fetch/map/notify phases.

## Design Principles Recap

- **Hexagonal:** Clean separation; core is framework-free.
- **Reactive:** Every IO path uses Mono/Flux.
- **Composable:** Processors plug into orchestration without core modification.
- **Extensible Notifications:** Enum + config-driven fan-out.

## Migration Playbook (High-Level)

1. Implement new platform module with minimal processor set.
2. Shadow-run in staging (dual publish to shadow topic/Webhook endpoint).
3. Validate counts & payload parity.
4. Enable in production; disable legacy microservice.
5. Monitor & iterate.

## Troubleshooting

| Symptom               | Possible Cause                                | Action                                                   |
|-----------------------|-----------------------------------------------|----------------------------------------------------------|
| No items processed    | DataProvider Flux completes early             | Inspect platform fetch logs; check pagination config.    |
| Notifications missing | Channel disabled or config null               | Check YAML under notifications; verify enable flag.      |
| High latency          | Excessive flatMap concurrency or API slowness | Tune concurrency limits; add resilience (retry/backoff). |
| DLQ backlog           | Kafka errors not transient                    | Inspect broker logs; evaluate payload schema issues.     |

## Contributing

1. Fork / branch.
2. Add or modify a port/adapter.
3. Include reactive tests (StepVerifier) for new flows.
4. Ensure architecture alignment (update ARCHITECTURE.md if contracts change).
5. Submit PR with summary and test evidence.

## License

Internal proprietary code (Glamaya). Not for external distribution.

