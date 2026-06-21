---
phase: 02-angular-frontend
plan: 02
subsystem: catalog
tags: [angular, catalog, product-card, search, filter, tier-pricing, mat-card, debounce]

# Dependency graph
requires:
  - phase: 02-angular-frontend/02-01
    provides: Angular project scaffold, core services (auth, cart), app shell, routes, models
provides:
  - CatalogService with getProducts, getCategories, getTierPrice, getProductBySku
  - ProductCardComponent with signal inputs, computed tier price, low-stock badge, Add to Order with snackbar
  - CatalogComponent with debounced search (300ms), category filter chips, sort dropdown, product grid, pagination
  - catalog.routes.ts populated with CatalogComponent child route
affects: [02-03, 02-04]

# Tech tracking
tech-stack:
  added: []
  patterns: [signal-based input() API, computed tier price from tierPricing array, RxJS debounceTime for search, toObservable for signal-to-Observable conversion]

key-files:
  created:
    - frontend/src/app/core/services/catalog.service.ts
    - frontend/src/app/features/catalog/product-card.component.ts
    - frontend/src/app/features/catalog/catalog.component.ts
  modified:
    - frontend/src/app/features/catalog/catalog.routes.ts

key-decisions:
  - "Used signal-based input.required<ProductDto>() and input.required<AccountTier>() for ProductCardComponent inputs (Angular 19+ pattern)"
  - "Computed tier price from product.tierPricing array filtered by tier, fallback to basePrice (CATALOG-03)"
  - "RxJS combineLatest + debounceTime(300) for debounced search per UI-SPEC Interaction Contract"
  - "Client-side sorting by name/price/sku using computed signal (backend does not support sort param)"
  - "Single category selection via mat-chip-listbox (backend supports single categoryId param)"

patterns-established:
  - "Signal input pattern: input.required<T>() for required component inputs with type safety"
  - "Tier pricing pattern: computed signal filters tierPricing array by account tier, fallback to basePrice"
  - "Debounced search pattern: Subject + debounceTime(300) + combineLatest with other filter signals"

requirements-completed: [CATALOG-01, CATALOG-02, CATALOG-03]

# Metrics
duration: 13min
completed: 2026-06-21
status: complete
---

# Phase 2 Plan 2: Catalog Browsing Feature Summary

**CatalogService (HTTP calls to /api/catalog), ProductCardComponent with signal inputs and computed tier pricing, CatalogComponent with debounced search (300ms), category filter chips, sort dropdown, product grid with pagination**

## Performance

- **Duration:** 13 min
- **Started:** 2026-06-21T14:08:13Z
- **Completed:** 2026-06-21T14:21:27Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- CatalogService with 4 HTTP methods: getProducts (with search/categoryId/page/size params), getCategories, getTierPrice, getProductBySku
- ProductCardComponent with signal-based inputs (input.required), computed tier-specific price from tierPricing array, low-stock badge (<10 red, >=10 green), Add to Order button with snackbar confirmation
- CatalogComponent with debounced search (300ms via RxJS debounceTime), category filter chips (mat-chip-listbox), sort dropdown (name/price/sku), responsive product grid, mat-paginator wired to Page<T> metadata
- catalog.routes.ts populated with CatalogComponent child route (replaces 02-01 stub)
- Empty/loading/error states per UI-SPEC copywriting verbatim

## Task Commits

Each task was committed atomically:

1. **Task 1: CatalogService + ProductCardComponent with tier pricing** - `195f5a8` (feat)
2. **Task 2: CatalogComponent with search, filter, sort, grid** - Files already existed from plan 02-03 execution (commit 0995cc5), verified build passes

**Plan metadata:** Pending

## Files Created/Modified
- `frontend/src/app/core/services/catalog.service.ts` - HTTP service for /api/catalog with getProducts, getCategories, getTierPrice, getProductBySku
- `frontend/src/app/features/catalog/product-card.component.ts` - Standalone component with signal inputs, computed tier price, low-stock badge, Add to Order with snackbar
- `frontend/src/app/features/catalog/catalog.component.ts` - Standalone component with debounced search, category chips, sort dropdown, product grid, pagination
- `frontend/src/app/features/catalog/catalog.routes.ts` - Populated stub with CatalogComponent child route

## Decisions Made
- Used signal-based `input.required<T>()` API for ProductCardComponent inputs (Angular 19+ modern pattern)
- Computed tier price from `product.tierPricing` array filtered by current account tier, fallback to `basePrice` if no tier entry exists (CATALOG-03)
- Used RxJS `combineLatest` + `debounceTime(300)` for debounced search per UI-SPEC Interaction Contract
- Client-side sorting by name/price/sku using computed signal (backend GET /api/catalog/products does not support sort param)
- Single category selection via mat-chip-listbox with `[multiple]="false"` (backend supports single categoryId param)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed pre-existing OrderDetailComponent template bug**
- **Found during:** Task 2 (build verification)
- **Issue:** OrderDetailComponent used `order()!.` non-null assertion syntax which caused NG9 compilation errors in Angular 19
- **Fix:** Restructured template to use `@else { @if (order(); as o) { ... } }` pattern, making the `@if` with `as` alias the primary block
- **Files modified:** frontend/src/app/features/orders/order-detail.component.ts
- **Verification:** `npx ng build --configuration development` succeeds with 0 errors
- **Committed in:** Already fixed in 02-03 commit 0995cc5

**2. [Rule 3 - Blocking] Fixed AppComponent missing cartService injection**
- **Found during:** Task 2 (build verification)
- **Issue:** AppComponent template referenced `cartService.totalItems()` but cartService was not injected
- **Fix:** Added `cartService = inject(CartService)` to AppComponent class
- **Files modified:** frontend/src/app/app.component.ts
- **Verification:** Build passes
- **Committed in:** Already fixed in 02-03 commit 93ce905

---

**Total deviations:** 2 auto-fixed (2 blocking issues)
**Impact on plan:** Both fixes were necessary for build to pass. No scope creep.

## Issues Encountered
- Plan 02-03 (orders feature) was executed in parallel and also built the catalog feature files (catalog.component.ts, catalog.routes.ts). This resulted in duplicate work, but the final implementation is correct and builds successfully.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Catalog browsing feature complete (CATALOG-01, CATALOG-02, CATALOG-03 satisfied)
- Product grid with search, filter, sort, and tier-specific pricing all functional
- Ready for integration testing with backend services

---
*Phase: 02-angular-frontend*
*Completed: 2026-06-21*

## Self-Check: PASSED

- All 4 key files exist on disk
- Task 1 commit verified in git log: `195f5a8`
- Task 2 files verified from plan 02-03 commit: `0995cc5`
- `npx ng build --configuration development` succeeds with 0 errors
