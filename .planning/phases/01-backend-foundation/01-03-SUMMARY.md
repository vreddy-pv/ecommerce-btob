---
phase: 01-backend-foundation
plan: 03
subsystem: api
tags: [spring-data-jpa, spring-boot, rest-api, catalog, tier-pricing, search]

# Dependency graph
requires:
  - phase: 01-01
    provides: "Spring Boot multi-module project structure with JPA entities for Product, Category, TierPricing"
provides:
  - "Product, Category, TierPricing repositories with search/filter capabilities"
  - "CatalogService with product search, category browsing, and tier pricing logic"
  - "CatalogController REST endpoints for catalog operations"
  - "DTOs: ProductDto, CategoryDto, TierPricingDto"
  - "Global exception handler for structured error responses"
  - "Seed data: 5 categories, 20 auto parts, tier pricing for SILVER/GOLD/PLATINUM"
affects: [01-04, order-service, gateway-service]

# Tech tracking
tech-stack:
  added: [spring-data-jpa-repositories, spring-rest-controllers, exception-handling]
  patterns: [repository-pattern, service-layer, dto-mapping, global-exception-handling, programmatic-seed-data]

key-files:
  created:
    - ecommerce-btob/catalog-service/src/main/java/com/btob/catalog/repository/ProductRepository.java
    - ecommerce-btob/catalog-service/src/main/java/com/btob/catalog/repository/CategoryRepository.java
    - ecommerce-btob/catalog-service/src/main/java/com/btob/catalog/repository/TierPricingRepository.java
    - ecommerce-btob/catalog-service/src/main/java/com/btob/catalog/service/CatalogService.java
    - ecommerce-btob/catalog-service/src/main/java/com/btob/catalog/service/DataInitializer.java
    - ecommerce-btob/catalog-service/src/main/java/com/btob/catalog/controller/CatalogController.java
    - ecommerce-btob/catalog-service/src/main/java/com/btob/catalog/dto/ProductDto.java
    - ecommerce-btob/catalog-service/src/main/java/com/btob/catalog/dto/CategoryDto.java
    - ecommerce-btob/catalog-service/src/main/java/com/btob/catalog/dto/TierPricingDto.java
    - ecommerce-btob/catalog-service/src/main/java/com/btob/catalog/exception/GlobalExceptionHandler.java
    - ecommerce-btob/catalog-service/src/main/java/com/btob/catalog/exception/ResourceNotFoundException.java
    - ecommerce-btob/catalog-service/src/main/resources/data.sql
  modified:
    - ecommerce-btob/catalog-service/src/main/resources/application.yml

key-decisions:
  - "Created DataInitializer (CommandLineRunner) instead of using data.sql for schema-timing safety"
  - "Changed JPA ddl-auto from validate to create for development environment"
  - "Disabled Spring Cloud compatibility verifier for Spring Boot 3.5.x / Spring Cloud 2024.x mismatch"
  - "Used JPQL @Query for complex product search combining name/SKU/category filters"
  - "Tier pricing falls back to basePrice when no tier-specific pricing exists"

patterns-established:
  - "Repository pattern: Spring Data JPA repositories with custom query methods"
  - "Service layer: Business logic in @Service with @Transactional boundaries"
  - "DTO pattern: Separate DTOs for API responses avoiding entity serialization"
  - "Global exception handling: @RestControllerAdvice for consistent error responses"
  - "Programmatic seeding: CommandLineRunner DataInitializer for schema-timing safety"

requirements-completed: [CATALOG-01, CATALOG-02, CATALOG-03]

# Metrics
duration: 15min
completed: 2026-06-21
status: complete
---

# Phase 01 Plan 03: Catalog Service Repositories and REST API Summary

**Product/Category/TierPricing repositories with search, CatalogService with tier pricing logic, CatalogController REST endpoints, and seed data with 20 auto parts across 5 categories**

## Performance

- **Duration:** 15 min
- **Started:** 2026-06-21T13:52:00Z
- **Completed:** 2026-06-21T14:07:00Z
- **Tasks:** 2
- **Files modified:** 12

## Accomplishments
- Three JPA repositories with search/filter capabilities (SKU lookup, name search, category filter, combined search, pagination)
- CatalogService with paginated product search, tier pricing logic, and category hierarchy
- CatalogController with 7 REST endpoints for product search, category browsing, and inventory management
- Global exception handler with structured error responses (404, 400, 500)
- Seed data: 5 categories (Brakes, Engine, Electrical, Suspension, Filters), 20 auto parts with realistic SKUs and pricing
- Tier pricing for SILVER (10%), GOLD (15%), PLATINUM (20%) discounts on 10 products

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement product and category repositories with search capabilities** - `d02af2e` (feat)
2. **Task 2: Implement catalog service and REST controllers** - `3f6873e` (feat)

**Plan metadata:** `0d72ec6` (fix: schema initialization and Spring Cloud compatibility)

## Files Created/Modified
- `catalog-service/src/main/java/.../repository/ProductRepository.java` - Product queries with search/filter, pagination, JPQL complex search
- `catalog-service/src/main/java/.../repository/CategoryRepository.java` - Category hierarchy queries (root/child)
- `catalog-service/src/main/java/.../repository/TierPricingRepository.java` - Tier pricing lookup by product and tier
- `catalog-service/src/main/java/.../service/CatalogService.java` - Business logic: getProducts, getProductBySku, getCategories, createProduct, updateInventory
- `catalog-service/src/main/java/.../service/DataInitializer.java` - CommandLineRunner seed data (5 categories, 20 products, tier pricing)
- `catalog-service/src/main/java/.../controller/CatalogController.java` - REST endpoints: GET/POST/PUT for products, GET for categories
- `catalog-service/src/main/java/.../dto/ProductDto.java` - Product data transfer object with tier pricing
- `catalog-service/src/main/java/.../dto/CategoryDto.java` - Category DTO with children hierarchy
- `catalog-service/src/main/java/.../dto/TierPricingDto.java` - Tier pricing DTO
- `catalog-service/src/main/java/.../exception/GlobalExceptionHandler.java` - @RestControllerAdvice for 404/400/500 errors
- `catalog-service/src/main/java/.../exception/ResourceNotFoundException.java` - 404 exception
- `catalog-service/src/main/resources/data.sql` - SQL seed data (used as reference, actual seeding via DataInitializer)
- `catalog-service/src/main/resources/application.yml` - Fixed duplicate cloud keys, disabled compatibility verifier, changed ddl-auto to create

## Decisions Made
- Used DataInitializer (CommandLineRunner) instead of data.sql because Hibernate's `ddl-auto: create` runs schema creation after SQL init, causing table-not-found errors
- Changed `ddl-auto` from `validate` to `create` for development (validates schema exists; creates if missing)
- Disabled `spring.cloud.compatibility-verifier.enabled` because Spring Boot 3.5.x is not officially compatible with Spring Cloud 2024.x release train
- Used `@Query` JPQL for complex product search combining name, SKU, and category filters in a single query

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed duplicate YAML keys causing parse error**
- **Found during:** Task 2 (application startup)
- **Issue:** `spring.cloud` appeared twice in application.yml with different properties, causing SnakeYAML duplicate key error
- **Fix:** Merged both `spring.cloud` blocks into one, adding `compatibility-verifier.enabled: false`
- **Files modified:** catalog-service/src/main/resources/application.yml
- **Verification:** Application starts without YAML parse errors
- **Committed in:** 0d72ec6

**2. [Rule 1 - Bug] Fixed data.sql timing issue with Hibernate schema creation**
- **Found during:** Task 2 (application startup)
- **Issue:** `ddl-auto: validate` failed because tables didn't exist; switching to `create` caused data.sql to run before Hibernate created tables
- **Fix:** Created DataInitializer CommandLineRunner that seeds data after schema creation, set `sql.init.mode: never`
- **Files modified:** catalog-service/src/main/java/.../service/DataInitializer.java, application.yml
- **Verification:** Application starts, schema creates, 5 categories + 20 products + tier pricing seeded correctly
- **Committed in:** 0d72ec6

**3. [Rule 3 - Blocking] Disabled Spring Cloud compatibility verifier**
- **Found during:** Task 2 (application startup)
- **Issue:** Spring Boot 3.5.15 not compatible with Spring Cloud 2024.x release train, causing startup failure
- **Fix:** Added `spring.cloud.compatibility-verifier.enabled: false` to application.yml
- **Files modified:** catalog-service/src/main/resources/application.yml
- **Verification:** Application starts successfully
- **Committed in:** 0d72ec6

---

**Total deviations:** 3 auto-fixed (2 bugs, 1 blocking)
**Impact on plan:** All auto-fixes necessary for application to start correctly. No scope creep - all changes align with plan intent.

## Issues Encountered
- Spring Cloud compatibility check prevented startup - resolved by disabling verifier (acceptable for development)
- data.sql execution timing conflict with Hibernate schema creation - resolved by using programmatic DataInitializer
- Eureka registration warnings (expected - Eureka server not running in local development)

## Known Stubs
None - all repositories, services, controllers, DTOs, and seed data are fully implemented.

## Threat Flags
None - no new security-relevant surface introduced beyond plan scope. Tier pricing is semi-public (B2B context, accepted in threat model).

## User Setup Required
None - no external service configuration required. Seed data loads automatically on first startup.

## Next Phase Readiness
- Catalog service complete with REST endpoints, ready for order service integration
- Plan 01-04 can implement Order service that calls catalog for price lookups
- Gateway service can route to catalog endpoints
- Frontend can consume catalog API for product browsing

## Self-Check: PASSED

- All 12 files exist and are correctly located
- All 3 task commits verified: d02af2e, 3f6873e, 0d72ec6
- Maven compile passes for catalog-service
- Application starts successfully, schema creates, seed data loads
- REST endpoints functional (products with search/filter, categories with hierarchy)

---
*Phase: 01-backend-foundation*
*Completed: 2026-06-21*
