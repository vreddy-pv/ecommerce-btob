# Phase 5: Inventory Management — Discussion CONTEXT

## Overview

Add proper inventory management to the B2B auto parts platform. Currently orders never decrement stock (mock data only), RabbitMQ events are published but never consumed, and there's no reservation, cancellation, alerting, or restock workflow.

## Locked Decisions

### D1: Decrement Strategy — Async event-driven
Complete the stubbed RabbitMQ pipeline.
- Add `Consumer<OrderCreatedEvent>` bean in catalog-service
- Add `spring.cloud.function.definition=orderConsumer` to catalog-service `application.yml`
- Event handler calls a new atomic reservation method
- Handle idempotent processing (event dedup via `orderId`)

### D2: Reservation Model — Soft reservation (B2B standard)
- Add `reservedInventory` field to `Product` entity alongside `inventoryLevel`
- On `OrderCreatedEvent`: increment `reservedInventory` (does NOT touch `inventoryLevel`)
- On shipment (CONFIRMED→SHIPPED): decrement `inventoryLevel`, decrement `reservedInventory`
- Available stock = `inventoryLevel - reservedInventory`
- On cancellation: decrement `reservedInventory` only (if PENDING) or both (if CONFIRMED)

### D3: Concurrency Control — Optimistic locking (`@Version`)
- Add `@Version private Long version;` to `Product` entity
- Retry on `OptimisticLockException` using Spring `@Retryable`
- For multi-item orders, retry entire batch if any item conflicts
- Also add `@Version` guard to admin `adjustInventory` endpoint

### D4: Cancellation/Restock — Full cancel + restore
- Add `CANCELLED` to `OrderStatus` enum
- Allowed transitions: `PENDING → CANCELLED`, `CONFIRMED → CANCELLED`
- New `OrderCancelledEvent` with line items (SKU + quantity)
- Published to `order-events` destination
- Catalog-service consumer:
  - If `PENDING → CANCELLED`: decrement `reservedInventory`
  - If `CONFIRMED → CANCELLED`: increment `inventoryLevel`, decrement `reservedInventory`
- New endpoint: `POST /api/orders/{id}/cancel` (auth required)
- Update `validateStatusTransition` to allow PENDING/CONFIRMED → CANCELLED

### D5: Low-Stock Alerting — Threshold + API + dashboard
- Add `reorderPoint` field to `Product` (Integer, nullable, default 10)
- New endpoint: `GET /api/catalog/products/low-stock` — returns products where `inventoryLevel - reservedInventory <= reorderPoint`
- Frontend dashboard widget: low-stock product table
- Frontend product card: fix out-of-stock display (currently shows "0 in stock" green when `< 10` — should show "Out of stock" in red)
- Chatbot MCP tool: `get_low_stock_items`

### D6: Restock & Receiving — Admin manual adjustment
- Fix `updateInventory` to delta-based: `adjustInventory(sku, delta)` where delta can be positive (restock) or negative (write-off)
- Change endpoint: `PATCH /api/catalog/products/{sku}/inventory` with JSON body `{"delta": 50}`
- Add auth (admin-only via gateway)
- Add `InventoryAdjustmentEvent` for audit trail (published to `catalog-events`)
- Admin frontend page: table of products with delta input and adjust button

## Key Constraints

| Area | Constraint |
|------|-----------|
| RabbitMQ | Use existing exchanges and bindings (`order-events`, `catalog-events`) |
| Idempotency | Event processing must be idempotent — track processed `orderId`s to handle retries |
| Auth | Inventory mutation endpoints require admin role; inventory read endpoints available to any authenticated user |
| DB | PostgreSQL, schema migrations via JPA ddl-auto (dev) or Flyway (future) |
| Frontend | Angular 19, standalone components, existing Material theme |

## Detailed Scope

### Catalog-service (backend)
- Product entity: add `reservedInventory`, `reorderPoint`, `@Version`
- ProductRepository: add atomic SQL reservation method (optional, `@Lock(PESSIMISTIC_WRITE)` for `findBySkuForUpdate`)
- CatalogService:
  - `reserveInventory(sku, quantity)` — increment reservedInventory, check available >= 0
  - `commitReservation(sku, quantity)` — move from reserved to consumed (decrement both)
  - `releaseReservation(sku, quantity)` — decrement reservedInventory only (cancel)
  - `adjustInventory(sku, delta)` — absolute set replaced by delta-based with version check
  - `getLowStockProducts()` — query products where available <= reorderPoint
- New `OrderCreatedEvent` consumer bean:
  - Function name: `orderCreatedConsumer`
  - Calls `reserveInventory` per line item
  - Tracks processed order IDs in a set (in-memory for now, persistent later)
- New `OrderCancelledEvent` consumer bean:
  - Same function bean, type-discriminated by checking event class or header
- Event classes (shared DTOs):
  - `OrderCreatedEvent` already exists (order-service)
  - `OrderCancelledEvent` needs creation (order-service)
  - `InventoryAdjustmentEvent` needs creation (catalog-service)
- Controller:
  - `GET /api/catalog/products/low-stock` — list low-stock products
  - `PATCH /api/catalog/products/{sku}/inventory` — delta-based adjust with auth

### Order-service (backend)
- Remove hardcoded static `PRODUCT_PRICES` and `PRODUCT_INVENTORY` maps
  - Replace with `RestTemplate`/`WebClient` call to catalog-service for price lookup
  - Keep as fallback if catalog-service is unreachable (fail-fast for now)
- Add `CANCELLED` to `OrderStatus`
- Update `validateStatusTransition`: allow PENDING → CANCELLED, CONFIRMED → CANCELLED
- New `OrderCancelledEvent` class with `orderId`, `accountId`, `items` (SKU + quantity), `reason`
- New event producer: `orderCancelled-out-0` → `order-events` destination
- New endpoint: `POST /api/orders/{id}/cancel`
- Add retry logic around `OptimisticLockException` (transient, retry 3 times with backoff)

### Frontend
- **Admin inventory page** (`/inventory`):
  - Table of all products with columns: SKU, Name, Inventory Level, Reserved, Available, Reorder Point, Actions
  - Action row: delta input (negative for adjustment, positive for restock) + "Adjust" button
  - Admin-only route guard
- **Dashboard low-stock widget**:
  - Card showing low-stock products (available <= reorderPoint)
  - Link to inventory management page
  - Refresh on page load
- **Product card fix**:
  - Show "Out of stock" (red) when `inventoryLevel <= 0`
  - Show "Low stock" (orange) when `available <= reorderPoint`
  - Show "In stock" (green) when `available > reorderPoint`
- **Cart validation**:
  - Before submitting order, call backend to verify available stock
  - Show inline errors per item if insufficient

### Chatbot (Python)
- New MCP tool in catalog-service: `get_low_stock_items`
  - Returns list of products where available <= reorderPoint
- New MCP tool in catalog-service: `check_stock(sku)`
  - Returns available stock for a given SKU

## Out of Scope (Deferred)
- Purchase Order (PO) workflow with suppliers
- Auto-expire stale PENDING reservations (scheduled job)
- Email notifications for low-stock alerts
- Multi-warehouse inventory
- Inventory audit log persistence (in-memory processed IDs only)
- Frontend real-time stock updates via WebSocket

## Files to Modify

| File | Change |
|------|--------|
| `catalog-service/.../entity/Product.java` | Add `reservedInventory`, `reorderPoint`, `@Version` |
| `catalog-service/.../repository/ProductRepository.java` | Add `findBySkuForUpdate`, low-stock query |
| `catalog-service/.../service/CatalogService.java` | Add reservation/commit/release/adjust methods |
| `catalog-service/.../controller/CatalogController.java` | Add low-stock endpoint, fix inventory endpoint to PATCH+delta |
| `catalog-service/.../event/InventoryAdjustmentEvent.java` | New event class |
| `catalog-service/.../service/OrderEventConsumer.java` | NEW — consumer for OrderCreatedEvent and OrderCancelledEvent |
| `catalog-service/src/main/resources/application.yml` | Add `spring.cloud.function.definition` |
| `order-service/.../entity/OrderStatus.java` | Add CANCELLED |
| `order-service/.../service/OrderService.java` | Remove mock data, add cancel logic, add retry |
| `order-service/.../controller/OrderController.java` | Add cancel endpoint |
| `order-service/.../event/OrderCancelledEvent.java` | New event class |
| `order-service/.../service/OrderEventPublisher.java` | Add cancel event publisher |
| `order-service/src/main/resources/application.yml` | Add `orderCancelled-out-0` binding |
| `frontend/src/app/core/services/catalog.service.ts` | Add low-stock, adjust-inventory methods |
| `frontend/src/app/core/services/order.service.ts` | Add cancel order method |
| `frontend/src/app/features/catalog/product-card.component.ts` | Fix stock display thresholds |
| `frontend/src/.../inventory/` | New admin inventory component |
| `frontend/src/.../dashboard/` | Low-stock widget on dashboard |
| `frontend/src/app/app.routes.ts` | Add inventory route (admin-only guard) |
| `chatbot-agents/` | No changes needed — MCP tools auto-discovered via SSE |
| `catalog-service/.../mcp/CatalogMcpTools.java` | Add `get_low_stock_items`, `check_stock` tools |

## Follow-up Questions Resolved
- Decrement strategy: ✅ Async event-driven
- Reservation model: ✅ Soft reservation
- Concurrency: ✅ Optimistic locking (@Version)
- Cancellation: ✅ Full cancel + restore
- Low-stock alerting: ✅ Threshold + API + dashboard + chatbot
- Restock/receiving: ✅ Admin manual adjustment (delta)
