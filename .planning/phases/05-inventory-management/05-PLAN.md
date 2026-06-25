# Phase 5: Inventory Management — PLAN.md

## Goal
Replace mock inventory with proper soft reservation system: reserve on order, commit on shipment, release on cancel, plus low-stock alerting and admin restock.

## Dependencies
- Phase 1 (Backend Foundation) — order-service and catalog-service REST APIs
- Phase 3 (MCP Server Integration) — MCP tools in both services
- Phase 4 (Chatbot UI & Agent Loop) — not required; MCP tools auto-discovered via SSE

## Requirements
- INV-01: Async event-driven decrement (RabbitMQ consumer in catalog-service)
- INV-02: Soft reservation model (reservedInventory field, reserve on order, commit on shipment)
- INV-03: Optimistic locking (@Version) with retry on OptimisticLockException
- INV-04: Full cancel+restore (CANCELLED status, OrderCancelledEvent, cancel endpoint)
- INV-05: Low-stock threshold querying via API, dashboard widget, chatbot MCP tool
- INV-06: Admin delta-based adjustInventory endpoint with auth + audit event

## Plans

### Plan 05-01: Catalog-service Core — Entity & Service Layer
**Goal**: Add `reservedInventory`, `reorderPoint`, `@Version` to Product entity; implement reserve/commit/release/adjustInventory service methods with optimistic locking retry

**Tasks**:
1. **Update `Product.java` entity**
   - Add `private Integer reservedInventory = 0;`
   - Add `private Integer reorderPoint = 10;`
   - Add `@Version private Long version;`
   - Update getter `getAvailableInventory()` → `inventoryLevel - reservedInventory`

2. **Update `ProductRepository.java`**
   - Add `@Lock(PESSIMISTIC_WRITE)` query: `findBySkuForUpdate(sku)` — optional, for high-contention paths
   - Add derived query: `findByReorderPointGreaterThanEqual()` for low-stock
   - Custom `@Query("SELECT p FROM Product p WHERE (p.inventoryLevel - p.reservedInventory) <= p.reorderPoint")`

3. **Update `CatalogService.java`** — add methods:
   - `reserveInventory(sku, quantity)` — increment reservedInventory, check available >= quantity, throw if insufficient
   - `commitReservation(sku, quantity)` — decrement both inventoryLevel and reservedInventory
   - `releaseReservation(sku, quantity, wasConfirmed)` — if confirmed: increment inventoryLevel + decrement reservedInventory; if pending: decrement reservedInventory only
   - `adjustInventory(sku, delta)` — delta-based (positive=restock, negative=write-off), calls save with @Version guard
   - `getLowStockProducts()` — returns products where available <= reorderPoint
   - `getAvailableStock(sku)` — returns available = inventoryLevel - reservedInventory

4. **Add retry infrastructure**
   - Add `@Retryable` / `@EnableRetry` annotation to service class or specific methods
   - Retry on `OptimisticLockException`, max 3 attempts with exponential backoff
   - For multi-item orders: retry entire batch if any item conflicts

5. **Add `spring-retry` dependency** to `catalog-service/pom.xml`

**Verification**:
- [ ] `mvn clean compile -pl catalog-service` passes
- [ ] `reserveInventory` correctly increments reservedInventory
- [ ] `reserveInventory` throws when available < quantity
- [ ] `commitReservation` decrements both fields by correct amounts
- [ ] `releaseReservation` handles both pending and confirmed variants
- [ ] `adjustInventory` correctly applies positive and negative deltas
- [ ] `getAvailableStock` returns correct available value
- [ ] OptimisticLockException triggers retry (max 3 attempts)

---

### Plan 05-02: Catalog-service Events, Controller & MCP Tools
**Goal**: Wire RabbitMQ consumers for OrderCreatedEvent/OrderCancelledEvent, add REST endpoints for low-stock and adjustInventory, and expose new MCP tools

**Tasks**:
1. **Create `InventoryAdjustmentEvent.java`** DTO
   - Fields: `sku`, `previousLevel`, `newLevel`, `delta`, `adminId`, `timestamp`

2. **Create `OrderEventConsumer.java`**
   - `@Bean` function `orderCreatedConsumer(OrderCreatedEvent event)` — calls `reserveInventory` per line item, tracks processed orderIds (in-memory Set for idempotency)
   - `@Bean` function `orderCancelledConsumer(OrderCancelledEvent event)` — calls `releaseReservation` per line item
   - Update `application.yml` — `spring.cloud.function.definition=orderCreatedConsumer;orderCancelledConsumer`

3. **Publish `InventoryAdjustmentEvent`** on adjustInventory success
   - Inject `StreamBridge` in CatalogService
   - Send to `catalog-events` destination

4. **Update `CatalogController.java`**
   - Add `GET /api/catalog/products/low-stock` — returns products where available <= reorderPoint
   - Change `PUT /api/catalog/products/{sku}/inventory` → `PATCH /api/catalog/products/{sku}/inventory` with `{"delta": N}` body
   - Add `GET /api/catalog/products/{sku}/stock` — returns available stock count

5. **Update `CatalogMcpTools.java`**
   - Add `@Tool("get_low_stock_items")` — returns low-stock product list
   - Add `@Tool("check_stock")` — returns available stock for a SKU

**Verification**:
- [ ] `mvn clean compile -pl catalog-service` passes
- [ ] `GET /api/catalog/products/low-stock` returns correct products
- [ ] `PATCH /api/catalog/products/{sku}/inventory` with delta succeeds
- [ ] RabbitMQ consumer triggers reserveInventory on OrderCreatedEvent
- [ ] RabbitMQ consumer triggers releaseReservation on OrderCancelledEvent
- [ ] Idempotent processing — duplicate events are ignored
- [ ] MCP tools `get_low_stock_items` and `check_stock` are discoverable

---

### Plan 05-03: Order-service Cancel Flow
**Goal**: Add CANCELLED status, cancel endpoint, OrderCancelledEvent, and remove hardcoded mock data

**Tasks**:
1. **Update `OrderStatus.java`**
   - Add `CANCELLED`

2. **Update `OrderService.java`**
   - Update `validateStatusTransition()` to allow: `PENDING → CANCELLED`, `CONFIRMED → CANCELLED`
   - Add `cancelOrder(orderId)` method:
     - Load order, validate current status allows cancel
     - Set status to CANCELLED
     - Publish `OrderCancelledEvent`
     - Save order
   - Remove hardcoded `PRODUCT_PRICES` and `PRODUCT_INVENTORY` maps
   - Replace price lookup with `RestTemplate`/`WebClient` call to `GET /api/catalog/products/{sku}` at catalog-service
   - Keep hardcoded fallback (fail-fast log warning)

3. **Create `OrderCancelledEvent.java`**
   - Fields: `orderId`, `accountId`, `items` (list of SKU+quantity), `reason`, `timestamp`

4. **Create event producer method in `OrderEventPublisher.java`**
   - `publishOrderCancelled(OrderCancelledEvent event)` → send to `order-events` destination via `orderCancelled-out-0`

5. **Update `OrderController.java`**
   - Add `POST /api/orders/{id}/cancel` — calls `orderService.cancelOrder(id)`
   - Auth required (valid JWT)

6. **Update `order-service/application.yml`**
   - Add `orderCancelled-out-0` binding under `spring.cloud.stream.bindings`

**Verification**:
- [ ] `mvn clean compile -pl order-service` passes
- [ ] `POST /api/orders/{id}/cancel` on PENDING order sets status to CANCELLED
- [ ] `POST /api/orders/{id}/cancel` on CONFIRMED order sets status to CANCELLED
- [ ] `POST /api/orders/{id}/cancel` on SHIPPED/DELIVERED order returns 400
- [ ] OrderCancelledEvent published to RabbitMQ on cancel
- [ ] Order creation uses catalog-service for price lookup (not hardcoded maps)
- [ ] Existing order creation still works end-to-end

---

### Plan 05-04: Frontend — Inventory, Low-Stock & Cancel UI
**Goal**: Add admin inventory management page, low-stock dashboard widget, fix product card stock display, and add cancel button to order detail

**Tasks**:
1. **Admin inventory page** (`/inventory`)
   - New standalone component: `InventoryListComponent`
   - Table of all products: SKU, Name, Inventory Level, Reserved, Available, Reorder Point, Actions
   - Action row: delta input (+ for restock, - for adjustment) + "Adjust" button
   - Calls `PATCH /api/catalog/products/{sku}/inventory`
   - Admin-only route guard (`authService.hasRole('ADMIN')`)
   - Add route to `app.routes.ts`

2. **Update `CatalogService` (frontend)**
   - Add `getLowStockProducts()` → `GET /api/catalog/products/low-stock`
   - Add `adjustInventory(sku, delta)` → `PATCH /api/catalog/products/{sku}/inventory`
   - Add `getProductStock(sku)` → `GET /api/catalog/products/{sku}/stock`

3. **Dashboard low-stock widget**
   - Add card to dashboard showing low-stock products
   - Columns: SKU, Name, Available Stock
   - Link to `/inventory` page
   - Refresh on component init

4. **Product card stock display fix**
   - `available = inventoryLevel - reservedInventory`
   - If `available <= 0`: "Out of stock" (red)
   - If `available <= reorderPoint`: "Low stock" (orange)
   - Else: "In stock" (green) with count

5. **Order cancel button**
   - Add "Cancel Order" button on order detail page
   - Visible only when status is `PENDING` or `CONFIRMED`
   - Confirmation dialog before proceeding
   - Calls `POST /api/orders/{id}/cancel`
   - Refresh order status after success

6. **Cart validation (pre-submit stock check)**
   - Before order submission, call `GET /api/catalog/products/{sku}/stock` for each item
   - Show inline error if any item has insufficient stock
   - Disable submit button if validation fails

**Verification**:
- [ ] `npm run build` passes (no TypeScript/Lint errors)
- [ ] Inventory page loads at `/inventory` (admin only, redirects for non-admin)
- [ ] Adjust inventory via delta input works and reflects immediately
- [ ] Dashboard shows low-stock widget with correct data
- [ ] Product card shows red "Out of stock" when available <= 0
- [ ] Product card shows orange "Low stock" when available <= reorderPoint
- [ ] Cancel button appears for PENDING/CONFIRMED orders
- [ ] Cancel button triggers confirmation and cancels order
- [ ] Cart validation blocks submission if stock insufficient

## Execution Order
05-01 → 05-02 → 05-03 → 05-04 (sequential)  
(05-02 depends on 05-01 service methods; 05-03 is independent of catalog-service changes and could run in parallel with 05-02)

## Estimated Effort
- Plan 05-01: ~1.5 hours
- Plan 05-02: ~1.5 hours
- Plan 05-03: ~1.5 hours
- Plan 05-04: ~2 hours
- **Total**: ~6.5 hours
