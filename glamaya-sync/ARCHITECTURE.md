# Glamaya Sync Service - Architectural Document

## 1. Vision & Goals

This document outlines the architecture for the new **`glamaya-sync`** service.

The primary goal is to evolve from multiple, separate synchronization microservices (`glamaya-woocommerce-sync`, `glamaya-wix-sync`) into a single, extensible, and maintainable **modular monolith (modulith)**.

### Key Objectives:
- **Reduce Code Duplication:** Centralize common business logic (sync orchestration, event publishing, status tracking) into a shared core.
- **Improve Extensibility:** Make onboarding new platforms (e.g., Shopify, Magento) a clean, low-effort process by adding new "plugin" modules without altering core logic.
- **Enhance Maintainability:** Establish a clear, consistent architectural pattern and separation of concerns, making the system easier to understand, test, and debug.
- **Increase Robustness:** Implement standardized, production-grade features like resilience, monitoring, and error handling across all integrations.
- **Zero-Downtime Migration:** Adopt a greenfield approach to build the new service in parallel with existing ones, allowing for a safe and phased rollout.

---

## 2. Architectural Pattern: Hexagonal Architecture

We will adopt the **Hexagonal Architecture (Ports and Adapters)** pattern. This pattern excels at decoupling the application's core business logic from external concerns (like APIs, frameworks, and databases).

- **The Core (The Hexagon):** Contains the pure business logic and has no dependencies on the outside world.
- **Ports:** Interfaces defined in the core that represent contracts for interaction.
    - **Inbound Ports (Use Cases):** Define how the outside world can interact with the core (e.g., `SyncPlatformUseCase`).
    - **Outbound Ports:** Define what the core needs from the outside world (e.g., `DataProvider`, `NotificationPort`).
- **Adapters:** Concrete implementations of the ports that bridge the core with external systems.
    - **Primary/Driving Adapters:** Drive the application (e.g., a scheduler, a REST controller).
    - **Secondary/Driven Adapters:** Are driven by the application (e.g., an API client, a database repository).

---

## 3. Project & Module Structure

The project will be a multi-module Maven/Gradle project to enforce separation of concerns at the build level.

```plaintext
glamaya-sync/
├── pom.xml                 (Parent POM defining dependencies and modules)
├── core/                   (The application's core logic and business rules)
├── platform-woocommerce/   (WooCommerce-specific adapter implementations)
├── platform-wix/           (Future Wix-specific adapter implementations)
└── runner/                 (The executable Spring Boot application)
```

### 3.1. `core` Module
The heart of the application. It is framework-independent and contains only pure business logic.
- **Dependencies:** No other project modules.
- **Contents:**
    - Canonical Domain Models (`GlamayaOrder`, `GlamayaProduct`).
    - Inbound and Outbound Port interfaces.
    - Core application services that orchestrate the sync process.

### 3.2. `platform-*` Modules (e.g., `platform-woocommerce`)
These are the "plugins" or adapters for each external platform.
- **Dependencies:** Depends only on the `core` module.
- **Contents:**
    - Implementations of the `DataProvider`, `DataMapper`, and other outbound ports.
    - Platform-specific API clients, authentication logic, and data models (DTOs).
    - Spring `@Configuration` to wire up the platform-specific beans.

### 3.3. `runner` Module
The executable entry point. It assembles the core and the desired platform adapters into a runnable application.
- **Dependencies:** Depends on `core` and all active `platform-*` modules.
- **Contents:**
    - The main `@SpringBootApplication` class.
    - Primary adapters, such as the `SyncScheduler`.
    - Configuration files (`application.yml` and profiles).

---

## 4. Detailed Package Structure

### `core` Module Packages
```plaintext
com.glamaya.sync.core/
├── application/
│   ├── service/      // SyncOrchestrationService (implements use case)
│   └── usecase/      // SyncPlatformUseCase (inbound port interface)
├── domain/
│   ├── model/        // Canonical models (GlamayaOrder, etc.)
│   └── port/
│       └── out/      // Outbound port interfaces (PlatformAdapter, DataProvider, StatusStorePort, etc.)
└── exception/        // Custom, generic exception types
```

### `platform-woocommerce` Module Packages
```plaintext
com.glamaya.sync.platform.woocommerce/
├── adapter/
│   ├── WooCommercePlatformAdapter.java // Main implementation of PlatformAdapter port
│   ├── client/       // WooCommerceApiClient (implements DataProvider port)
│   └── mapper/       // WooCommerceOrderMapper (implements DataMapper port)
├── config/           // WooCommerceModuleConfiguration (Spring @Configuration)
└── model/            // WooCommerce-specific DTOs
```

### `runner` Module Packages
```plaintext
com.glamaya.sync.runner/
├── GlamayaSyncApplication.java
└── scheduler/
    └── SyncScheduler.java // Primary adapter
```

---

## 5. Core Ports and Domain Models

This section details the primary interfaces (ports) and models that form the contract between the `core` module and the outside world.

### 5.1. `PlatformAdapter` (Outbound Port)
The main "plugin" contract that each platform module must implement. It's the entry point for the core's orchestration service to trigger a platform's sync process.

```java
public interface PlatformAdapter {
    String getPlatformName();
    void sync();
}
```

### 5.2. `DataProvider<T>` (Outbound Port)
Defines the contract for fetching raw data from a platform's API.
- `T`: The platform-specific DTO type.

```java
public interface DataProvider<T> {
    List<T> fetchData(SyncContext context);
}
```

### 5.3. `DataMapper<P, C>` (Outbound Port)
Defines the contract for converting a platform-specific model (`P`) into a canonical core domain model (`C`).

```java
public interface DataMapper<P, C> {
    C mapToCanonical(P platformModel);
}
```

### 5.4. `StatusStorePort` (Outbound Port)
Defines the contract for persisting and retrieving the state of a sync process, enabling incremental syncs.

```java
public interface StatusStorePort {
    Optional<ProcessorStatus> findStatus(ProcessorType processorType);
    void saveStatus(ProcessorStatus status);
}
```

### 5.5. `NotificationPort<C>` (Outbound Port)
A generic contract for dispatching a canonical domain model to one or more external systems (e.g., Kafka, Webhooks). See Section 8 for the composite implementation details.

```java
public interface NotificationPort<C> {
    void notify(C payload);
    boolean supports(C payload);
}
```

### 5.6. Supporting Domain Models

- **`SyncContext`**: A record passed to `DataProvider` containing the state needed for an API call.
  ```java
  public record SyncContext(ProcessorStatus status, Map<String, Object> configuration) {}
  ```
- **`ProcessorStatus`**: A class holding the state of a single sync process (e.g., for "WooCommerce Orders").
  ```java
  public class ProcessorStatus {
      private ProcessorType processorType;
      private java.time.Instant lastSuccessfulRun;
      private String cursor;
      private int currentPage;
      // Getters & Setters
  }
  ```
- **`ProcessorType`**: An enum that uniquely identifies each distinct sync process.
  ```java
  public enum ProcessorType {
      WOOCOMMERCE_ORDER,
      WOOCOMMERCE_PRODUCT,
      WIX_CONTACT
  }
  ```

---

## 6. Key Features & Responsibilities

| Feature                 | Primary Responsibility                                       | Rationale                                                                                             |
|-------------------------|--------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| **Sync Orchestration**  | `core`                                                       | The business process of syncing is generic.                                                           |
| **Authentication**      | `platform-*`                                                 | Each platform has a unique authentication mechanism (OAuth1, OAuth2, API Keys).                       |
| **Resilience**          | `core` (Capability), `platform-*` (Configuration)            | `core` provides the tools (Resilience4j). Each platform configures its own rules (rate limits, thresholds). |
| **API Client Logic**    | `platform-*`                                                 | API endpoints, request/response formats are platform-specific.                                        |
| **Data Mapping**        | `platform-*`                                                 | Logic to convert platform models to the canonical domain model is specific to each platform.          |
| **Notification System** | `core` (Port), `runner` (Composite & Leaf Adapters)          | `core` defines the need to notify. The `runner` provides a composite adapter that can fan-out to multiple destinations (Kafka, Webhooks, etc.). |
| **Status Tracking**     | `core` (Port), `runner` (Adapter)                            | `core` defines the need to store status. The `runner` provides the database implementation.           |
| **Logging & Metrics**   | `core` (Capability), `platform-*` & `runner` (Implementation)| `core` provides common logging/metrics tools. Adapters add specific context.                          |
| **Scheduling**          | `runner`                                                     | How and when the sync is triggered is a delivery detail, not a core business rule.                    |
| **Configuration**       | `runner`                                                     | Managed via Spring Profiles (`application-woocommerce.yml`) to keep platform settings separate.       |

---

## 7. Phased Development & Rollout Plan

We will follow a greenfield approach, creating the `glamaya-sync` project from scratch to avoid disrupting live services.

### Phase 1: Foundation & WooCommerce Implementation
1.  **Setup:** Create the new multi-module `glamaya-sync` project with the `core`, `platform-woocommerce`, and `runner` modules.
2.  **Core Logic:** Build out the core interfaces (ports), domain models, and the generic `SyncOrchestrationService`.
3.  **WooCommerce Adapter:** Implement the `platform-woocommerce` module, porting and cleaning the logic from the existing `glamaya-woocommerce-sync` service.
4.  **Validation:** Run the new service in a staging environment. Validate its behavior against the existing service, potentially by writing to a temporary Kafka topic or database. Ensure metrics and logs are correct.

### Phase 2: Production Cutover for WooCommerce
1.  **Deploy:** Deploy the `glamaya-sync` application to production, configured to run only the WooCommerce sync.
2.  **Cutover:** Once confident in its stability and correctness, disable the old `glamaya-woocommerce-sync` service. The new service now handles all WooCommerce synchronization.
3.  **Monitor:** Closely monitor the new service's performance, metrics, and error rates.

### Phase 3: Wix Implementation & Decommission
1.  **Wix Adapter:** Create a new `platform-wix` module within the `glamaya-sync` project.
2.  **Implement:** Port the logic from the existing `glamaya-wix-sync` service into the new adapter, following the established patterns.
3.  **Validate & Cutover:** Follow the same validation and cutover process as in Phase 1 & 2 for the Wix integration.
4.  **Decommission:** Once the Wix sync is running successfully on the new service, the old `glamaya-wix-sync` microservice can be decommissioned.

### Phase 4: Future Platforms
- Onboarding a new platform (e.g., Shopify) now follows a clean, repeatable process:
    1. Create a new `platform-shopify` module.
    2. Implement the required port interfaces from `core`.
    3. Add an `application-shopify.yml` configuration file.
    4. Deploy and enable the new profile.

---

## 8. Composite Notification System (Fan-Out)

To meet the requirement of publishing a single canonical event to multiple destinations simultaneously (e.g., to Kafka and a webhook like n8n), the system will implement a "fan-out" capability using the **Composite Design Pattern**.

This pattern allows the `core` module to remain completely decoupled, interacting with a single `NotificationPort` interface, while the `runner` module assembles a chain of publishers behind the scenes.

### 8.1. The Generic `NotificationPort`

The `NotificationPort` (detailed in Section 5.5) is the cornerstone of this pattern. It represents a generic contract to dispatch a canonical model to any external system.

### 8.2. Leaf Adapters

For each destination, a specific "leaf" adapter will be created in the `runner` module (or a dedicated adapter module). Each adapter implements `NotificationPort` for its specific task.

**Example: Kafka Adapter**
```java
@Service
@ConditionalOnProperty("glamaya.notifications.kafka.enabled")
public class KafkaNotificationAdapter implements NotificationPort<Object> {
    // ... KafkaTemplate logic ...
}
```

**Example: Webhook Adapter**
```java
@Service
@ConditionalOnProperty("glamaya.notifications.webhook.enabled")
public class WebhookNotificationAdapter implements NotificationPort<GlamayaOrder> {
    // ... WebClient logic for posting to a webhook ...
}
```

### 8.3. The Composite Adapter

A special composite adapter is created in the `runner` module. It also implements `NotificationPort`, but its job is to delegate the `notify` call to all other `NotificationPort` beans.

```java
@Service
@Primary // Ensures this is the default implementation injected
public class CompositeNotificationAdapter implements NotificationPort<Object> {

    private final List<NotificationPort<Object>> notifiers;

    // Spring injects all NotificationPort beans (including this one)
    public CompositeNotificationAdapter(List<NotificationPort<Object>> notifiers) {
        // Filter self out to prevent infinite recursion
        this.notifiers = notifiers.stream()
                                  .filter(n -> !(n instanceof CompositeNotificationAdapter))
                                  .toList();
    }

    @Override
    public void notify(Object payload) {
        for (NotificationPort<Object> notifier : notifiers) {
            if (notifier.supports(payload)) {
                notifier.notify(payload);
            }
        }
    }
    // ...
}
```

### 8.4. Workflow

1.  The `SyncOrchestrationService` in `core` is injected with a `NotificationPort`.
2.  Due to the `@Primary` annotation, Spring provides the `CompositeNotificationAdapter`.
3.  The `core` calls `notificationPort.notify(payload)`.
4.  The `CompositeNotificationAdapter` receives the call and iterates through its list of leaf adapters (`KafkaNotificationAdapter`, `WebhookNotificationAdapter`, etc.).
5.  For each leaf, it checks if it `supports()` the payload and, if so, calls its `notify()` method.

### 8.5. Configuration

This design enables highly flexible, flag-based configuration in `application.yml`.

```yaml
glamaya:
  notifications:
    kafka:
      enabled: true
      # ... kafka-specific properties
    webhook:
      enabled: true
      # ... webhook-specific properties
    email:
      enabled: false # Future notifier can be added and enabled easily
```

This architecture provides a robust, scalable, and maintainable foundation for all current and future data synchronization needs at Glamaya.
