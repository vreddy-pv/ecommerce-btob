---
phase: 05
plan: 02
subsystem: catalog-service
tags: ["inventory", "events", "rabbitmq", "mcp", "rest"]
requires: ["05-01"]
provides: ["catalog-order-events", "inventory-mcp-tools"]
affects: ["order-service"]
tech-stack:
  added: ["spring-cloud-function", "java.util.function.Consumer", "StreamBridge", "ConcurrentHashMap"]
  patterns: ["idempotent-event-consumer", "event-publishing-on-state-change"]
key-files:
  created:
    - catalog-service/src/main/java/com/btob/catalog/event/InventoryAdjustmentEvent.java
    - catalog-service/src/main/java/com/btob/catalog/event/OrderCreatedEvent.java
    - catalog-service/src/main/java/com/btob/catalog/event/OrderCancelledEvent.java
    - catalog-service/src/main/java/com/btob/catalog/service/OrderEventConsumer.java
  modified:
    - catalog-service/src/main/java/com/btob/catalog/service/CatalogService.java
    - catalog-service/src/main/java/com/btob/catalog/controller/CatalogController.java
    - catalog-service/src/main/java/com/btob/catalog/mcp/CatalogMcpTools.java
    - catalog-service/src/main/resources/application.yml
decisions:
  - "Local event copies: catalog-service maintains local copies of OrderCreatedEvent and OrderCancelledEvent for RabbitMQ deserialization since there is no shared event module."
  - "ConcurrentHashMap-backed Set for idempotency: uses ConcurrentHashMap.newKeySet() for thread-safe duplicate event detection."
  - "orderCancelled-in-0 binding: added via Rule 2 since the function definition referenced orderCancelledConsumer but no binding existed."
  - "PATCH replaces PUT: inventory adjustment uses PATCH with delta body instead of PUT with absolute quantity for semantic correctness."
metrics:
  duration: ~15 minutes
  completed_date: 2026-06-25
  commits: 7
status: complete
---

# Phase 5 Plan 2: Catalog-service Events, Controller & MCP Tools

Wire RabbitMQ consumers for order lifecycle events, add REST endpoints for low-stock and delta-based inventory adjustment, and expose new MCP tools for AI agent access.

## Tasks Completed

| # | Task | Type | Commit |
|---|------|------|--------|
| 1 | Create `InventoryAdjustmentEvent.java` DTO | auto | 642ddd7 |
| 2 | Create `OrderEventConsumer.java` + local event copies | auto | 2dd43cd |
| 3 | Publish `InventoryAdjustmentEvent` on adjustInventory success | auto | 4f14a84 |
| 4 | Update `CatalogController.java` (low-stock, stock check, PATCH inventory) | auto | eb389c7 |
| 5 | Update `CatalogMcpTools.java` (get_low_stock_items, check_stock) | auto | b9ef317 |
| 6 | Update `application.yml` (function definition) | auto | 7d89ef7 |

## Deviations from Plan

### Rule 2 — Missing Critical Functionality: orderCancelled-in-0 binding

- **Found during:** Task 6 (post-commit review)
- **Issue:** The `spring.cloud.function.definition` included `orderCancelledConsumer`, but no corresponding RabbitMQ binding existed. Without a binding, the consumer bean would never receive cancellation events from RabbitMQ.
- **Fix:** Added `orderCancelled-in-0` binding to `order-cancelled-events` destination in `application.yml`. This matches the naming pattern established by `orderCreated-in-0`.
- **Files modified:** `catalog-service/src/main/resources/application.yml`
- **Commit:** ec233c2

## Architecture Decisions

### Local Event Copies (no shared module)
Since there is no shared event library module, each service maintains local copies of event DTOs. The field names must match exactly for proper JSON deserialization across RabbitMQ. The catalog-service copies:
- `OrderCreatedEvent` — matches order-service structure (with full `OrderItemEvent` including `unitPrice`)
- `OrderCancelledEvent` — defined locally with `wasConfirmed` boolean for proper inventory release semantics

### Idempotent Event Processing
`OrderEventConsumer` uses `ConcurrentHashMap.newKeySet()` to track processed order IDs. This provides thread-safe duplicate detection across concurrent invocations. Each event consumer checks and adds the order ID before processing, logging a warning on duplicates.

### Delta-based PATCH Endpoint
Changed from `PUT /.../inventory?quantity=N` (absolute value) to `PATCH /.../inventory` with `{"delta": N}` body. Positive delta = restock, negative delta = write-off. This is semantically correct (PATCH for partial update) and matches the `CatalogService.adjustInventory()` signature.

## Key Components

### New Files

| File | Purpose |
|------|---------|
| `event/InventoryAdjustmentEvent.java` | Event payload for inventory adjustments (sku, delta, levels, timestamp) |
| `event/OrderCreatedEvent.java` | Local copy for deserializing order creation events from order-service |
| `event/OrderCancelledEvent.java` | Local copy for deserializing order cancellation events |
| `service/OrderEventConsumer.java` | RabbitMQ consumer with idempotent processing of order lifecycle events |

### Modified Files

| File | Change |
|------|--------|
| `service/CatalogService.java` | Added StreamBridge; adjustInventory() publishes InventoryAdjustmentEvent |
| `controller/CatalogController.java` | Added GET /low-stock, GET /{sku}/stock; changed PUT to PATCH with delta body |
| `mcp/CatalogMcpTools.java` | Added get_low_stock_items() and check_stock() MCP tools |
| `application.yml` | Added function definition + orderCancelled-in-0 binding |

## Verification

- [x] `mvn compile -pl catalog-service` passes
- [x] GET /api/catalog/products/low-stock returns List<ProductDto>
- [x] GET /api/catalog/products/{sku}/stock returns {"available": N}
- [x] PATCH /api/catalog/products/{sku}/inventory accepts delta body, returns updated ProductDto
- [x] MCP tools get_low_stock_items and check_stock are discoverable via @Tool annotations

## Commits

| Hash | Message |
|------|---------|
| 642ddd7 | feat(catalog): add InventoryAdjustmentEvent DTO [05-02] |
| 2dd43cd | feat(catalog): add order event consumer with idempotent processing [05-02] |
| 4f14a84 | feat(catalog): publish InventoryAdjustmentEvent on inventory adjustment [05-02] |
| eb389c7 | feat(catalog): add low-stock and stock-check endpoints, PATCH inventory [05-02] |
| b9ef317 | feat(catalog): add MCP tools for low-stock check and stock query [05-02] |
| 7d89ef7 | feat(catalog): configure function definition for order event consumers [05-02] |
| ec233c2 | fix(catalog): add orderCancelled-in-0 binding for cancellation consumer [05-02] |

## Duration

~15 minutes of execution time on 2026-06-25.

## Self-Check: PASSED

All 7 commits verified in git log. All 8 files confirmed created/modified. Maven compile passes.
