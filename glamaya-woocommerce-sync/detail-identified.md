# Detailed Findings: glamaya-woocommerce-sync

## Project Overview
- **Type:** Spring Boot microservice
- **Purpose:** Synchronizes WooCommerce data (orders, products, users) with other systems via polling, event publishing, and notifications.
- **Reactive:** Uses Project Reactor (Mono, Flux) and Spring WebFlux for non-blocking IO.
- **Persistence:** Uses MongoDB (reactive driver) for status tracking.
- **Integration:** Publishes events to Kafka, notifies via n8n webhooks, and exposes actuator endpoints.
- **Configuration:** Uses YAML files for environment and application settings, with many @Value annotations in code.
- **Build:** Maven, Java 21, Dockerized for deployment.

## Key Packages & Classes
- **com.glamaya.glamayawoocommercesync.processor**
  - `AbstractWooProcessor<E>`: Abstract base for polling, fetching, and processing WooCommerce entities. Handles polling cadence, OAuth, tracker persistence, and event publishing hooks.
  - `OrderProcessor`, `ProductProcessor`, `UserProcessor`: Concrete processors for each entity type. Implement hooks for event publishing, formatting, and notifications.
  - `GlamWoocommerceProcessor`: Interface for processors, extending Spring Integration's GenericHandler.
- **com.glamaya.glamayawoocommercesync.monitoring**
  - `MetricsEventListener`: Listens for fetch cycle events, records metrics with Micrometer.
  - `FetchCycleEvent`: Event for monitoring polling/fetching cycles.
- **com.glamaya.glamayawoocommercesync.util**
  - `ModifiedDateResolver`: Utility for resolving the most appropriate modified date from WooCommerce entities.
- **Configuration**
  - `application.yml`, `application-local.yml`: Define MongoDB, Kafka, external API, and application-specific settings.
  - Many settings are injected via @Value in code, not centralized.
- **Testing**
  - Only a single test class exists: `GlamayaWoocommerceSyncApplicationTests` (context load test only).
- **Build & Deployment**
  - Maven build, Dockerfile for containerization, uses Java 21.

## Design & Coding Practices
- **Reactive/Async:** Processors use non-blocking, reactive patterns for fetching and processing data.
- **Event-Driven:** Publishes events to Kafka and notifies via webhooks.
- **Metrics:** Uses Micrometer for custom metrics on fetch cycles.
- **Logging:** Uses SLF4J for logging, but log messages are not always standardized.
- **Exception Handling:** No global exception handler; error handling is mostly local to processors.
- **Configuration:** Scattered @Value usage, not centralized in configuration classes.
- **Validation:** No explicit input validation or sanitization for external data.
- **Documentation:** Minimal JavaDoc/comments on public APIs and classes.
- **Code Style:** No evidence of static analysis or code style enforcement in the build.

## Summary of Identified Issues
1. **Configuration is not centralized**; heavy use of @Value instead of @ConfigurationProperties.
2. **Low test coverage**; only a context load test exists.
3. **Processors have multiple responsibilities**; need for SRP refactoring.
4. **No global exception handling**; error handling is ad hoc.
5. **Logging is inconsistent**; lacks correlation/tracing.
6. **Minimal documentation**; public APIs/classes lack JavaDoc.
7. **No explicit input validation/sanitization** for external data.
8. **Reactive code could be optimized** for error handling and resource management.
9. **No static analysis/code style enforcement** in CI/CD.
10. **Design patterns (Strategy, Factory) could be leveraged** for extensibility.

## How to Use This File
- This file summarizes the architecture, design, and current state of the glamaya-woocommerce-sync project.
- If you need to perform enhancements, refactoring, or debugging, refer to this file for a high-level understanding of the codebase and its issues.
- For enhancement tasks, see the `todo.md` file for actionable items.

