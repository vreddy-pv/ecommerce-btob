# Plan 05-01 SUMMARY: Catalog-service Core — Entity & Service Layer

## Commits
1. `6f80cea` — Add reservedInventory, reorderPoint, @Version to Product entity
2. `574bcec` — Add findBySkuForUpdate and low-stock queries to ProductRepository
3. `3290906` — Add spring-retry dependency and @EnableRetry
4. `fcab516` — Add reservedInventory and reorderPoint to ProductDto
5. `32f57c9` — Implement reserveInventory, commitReservation, releaseReservation, adjustInventory, getLowStockProducts, getAvailableStock with optimistic locking retry

## Verification
- [x] `mvn compile -pl catalog-service` passes
- [x] Product entity has reservedInventory, reorderPoint, @Version
- [x] ProductRepository has findBySkuForUpdate (pessimistic write), findLowStockProducts (JPQL)
- [x] CatalogService has reserveInventory, commitReservation, releaseReservation, adjustInventory, getLowStockProducts, getAvailableStock
- [x] All mutation methods annotated @Transactional, @Retryable on OptimisticLockException
- [x] spring-retry dependency added to pom.xml, @EnableRetry configured

## Files Modified
- catalog-service/src/main/java/com/btob/catalog/entity/Product.java
- catalog-service/src/main/java/com/btob/catalog/repository/ProductRepository.java
- catalog-service/pom.xml
- catalog-service/src/main/java/com/btob/catalog/service/CatalogService.java
- catalog-service/src/main/java/com/btob/catalog/dto/ProductDto.java
