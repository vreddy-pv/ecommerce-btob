# Plan 05-03 SUMMARY: Order-service Cancel Flow

## Commits
1. `bfdf012` — Add CANCELLED status to OrderStatus, create OrderCancelledEvent
2. `f16d566` — Add publishOrderCancelled method to OrderEventPublisher
3. `69d7a0f` — Add cancelOrder method to OrderService, remove mock data maps, inject RestTemplate for catalog-service price lookup
4. `b1f00e1` — Add POST /api/orders/{id}/cancel endpoint to OrderController
5. `b323ffe` — Add orderCancelled-out-0 binding to application.yml

## Verification
- [x] `mvn compile -pl order-service` passes
- [x] CANCELLED added to OrderStatus enum
- [x] OrderCancelledEvent created with items, wasConfirmed flag
- [x] validateStatusTransition allows PENDING→CANCELLED and CONFIRMED→CANCELLED
- [x] cancelOrder publishes OrderCancelledEvent with correct wasConfirmed
- [x] Hardcoded PRODUCT_PRICES and PRODUCT_INVENTORY maps removed
- [x] Price lookup calls catalog-service REST API with RestTemplate fallback
- [x] Cancel endpoint at POST /api/orders/{id}/cancel
- [x] Application.yml has orderCancelled-out-0 binding

## Files Modified/Created
- order-service/src/main/java/com/btob/order/entity/OrderStatus.java (modified)
- order-service/src/main/java/com/btob/order/event/OrderCancelledEvent.java (created)
- order-service/src/main/java/com/btob/order/service/OrderEventPublisher.java (modified)
- order-service/src/main/java/com/btob/order/service/OrderService.java (modified)
- order-service/src/main/java/com/btob/order/config/AppConfig.java (created)
- order-service/src/main/java/com/btob/order/controller/OrderController.java (modified)
- order-service/src/main/resources/application.yml (modified)
