# Plan 02-01: Angular Project Setup & Core Services — Summary

**Plan ID:** 02-01
**Phase:** 02-Angular Frontend
**Wave:** 1
**Status:** Complete
**Completed:** 2026-06-21

## Objective
Scaffold the Angular SPA in `frontend/`, wire up core services, interceptors, guards, app shell, and gateway CORS + static serving.

## What Was Built

### Task 1: Scaffold Angular Project
- Created Angular project in `frontend/` with Angular Material
- Feature folder structure: `core/`, `features/`, `shared/`
- Core models (`api.models.ts`), auth interceptor, error interceptor, auth guard
- All standalone components (no NgModules per D-01)

### Task 2: App Shell & Core Services
- `AuthService` — login, register, JWT token management (localStorage)
- `CartService` — cart state management with Signals
- `authGuard` — route protection based on auth state
- App shell: sidebar (240px) + header (64px) per UI-SPEC
- Login and Register components
- Feature route files (stub `catalog.routes.ts`, `orders.routes.ts`)
- Final `app.routes.ts` with `loadChildren` entries for both features

### Task 3: Gateway CORS + Static Resource Serving
- `CorsConfig.java` — CORS configuration for frontend origin
- `StaticResourceConfig.java` — WebFlux static resource serving with SPA fallback
- Updated `application.yml` with static resource location config
- Dev proxy (`proxy.conf.json`) for local development

## Files Created/Modified
- `frontend/` — entire Angular project (23+ files)
- `gateway-service/src/main/java/com/btob/gateway/config/CorsConfig.java`
- `gateway-service/src/main/java/com/btob/gateway/config/StaticResourceConfig.java`
- `gateway-service/src/main/resources/application.yml`

## Verification
- `npx ng build` — **PASS** (builds successfully, lazy chunks for catalog/orders routes)
- Feature folder structure verified
- All standalone components confirmed

## Commits
- `35923df` feat(02-01): scaffold Angular 19 project with Material, core models, interceptors, guard, dev proxy
- `061c306` feat(02-01): add app shell, auth components, cart service, feature routes, gateway CORS
- `fcd02cc` feat(02-01): add WebFlux static resource serving + SPA fallback for gateway (D-04)

## Notes
- Angular 19 used (latest stable available via CLI; plans said 22 but 19 is what's installed)
- Bundle size warning (686KB > 500KB budget) — non-blocking, can optimize later
- Lazy loading working: catalog-routes and orders-routes are separate chunks
