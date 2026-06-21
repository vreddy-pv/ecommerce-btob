# Phase 2: Angular Frontend - Context

**Gathered:** 2026-06-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Build a B2B dashboard with Angular 17+ that displays the auto parts catalog with search and filtering, shows recent orders with status indicators, and supports order creation. The frontend consumes REST APIs from the Spring Boot backend via the gateway. Visual and interaction design is locked by the UI-SPEC.md (Angular Material 17, MD3, sidebar+header layout).

</domain>

<decisions>
## Implementation Decisions

### Component Architecture
- **D-01:** Use Angular 17+ standalone components (no NgModules) — modern Angular default, simpler imports, lazy-loadable by default, less boilerplate
- **D-02:** Use feature folder structure — `features/catalog/`, `features/orders/`, `features/auth/`, `core/services/`, `shared/components/`. Each feature is self-contained for maintainability and lazy loading

### Project Location
- **D-03:** Frontend lives in `frontend/` subdirectory inside the existing project root. Keeps everything in one repo alongside account-service/, catalog-service/, order-service/, gateway-service/

### Serving Strategy
- **D-04:** Configure CORS on the Spring Boot gateway and serve Angular static files from gateway-service. This couples frontend serving to the backend but simplifies the Docker Compose setup (no separate Nginx container needed). The gateway serves the built Angular files as static resources and proxies API calls to backend services

### the agent's Discretion
- State management approach (Signals vs NgRx vs RxJS/BehaviorSubject) — user did not select this area for discussion; planner/researcher should recommend based on Angular 17 best practices and cart/auth requirements
- Auth & API integration details (JWT storage, HTTP interceptor, token refresh, error handling) — user did not select this area; researcher should investigate and planner should decide
- Routing structure and auth guards — user did not select this area; planner should design based on UI-SPEC layout and feature folders
- Angular CLI version (17.x or 18.x), build tooling, testing framework — researcher should recommend latest stable

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### UI Design Contract
- `.planning/phases/02-angular-frontend/02-UI-SPEC.md` — Visual and interaction contract: Angular Material 17, MD3, spacing scale, typography, color 60/30/10, component inventory, copywriting, accessibility requirements. ALL visual decisions are locked here.

### Project Context
- `.planning/PROJECT.md` — Project goals, constraints, key decisions (Angular 17+, Docker Compose, JWT auth)
- `.planning/REQUIREMENTS.md` — v1 requirements: CATALOG-01/02/03 (catalog display, search/filter, tier pricing), ORDER-01/02/03 (order creation, status tracking, order history)
- `.planning/ROADMAP.md` — Phase 2 goal and success criteria (3 criteria: catalog with search/filter, orders table with status, navigation between views)

### Phase 1 Context (Backend APIs)
- `.planning/phases/01-backend-foundation/01-CONTEXT.md` — Backend decisions: microservice structure, Spring Cloud Gateway, JWT auth, event-driven messaging

### Backend API Surface
- `gateway-service/src/main/resources/application.yml` — Gateway routes: `/api/accounts/**`, `/api/auth/**` → account-service:8081; `/api/catalog/**` → catalog-service:8082; `/api/orders/**` → order-service:8083
- 18 REST endpoints available via gateway on port 8080 (see Phase 1 context and backend controllers)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- None — this is a greenfield frontend. No Angular code exists yet.

### Established Patterns
- Backend uses Spring Cloud Gateway for routing (port 8080) — frontend will call gateway endpoints
- JWT auth with 24h expiry, BCrypt password hashing
- Account tiers: STANDARD, SILVER, GOLD, PLATINUM (affects tier pricing display)
- Order statuses: PENDING, CONFIRMED, SHIPPED, DELIVERED
- Backend uses UUIDs for all entity IDs

### Integration Points
- Gateway endpoint: `http://localhost:8080/api/*` — all API calls go through gateway
- Auth endpoints: `POST /api/auth/login`, `POST /api/auth/register` — returns JWT token
- Catalog endpoints: `GET /api/catalog/products` (search, categoryId, page, size params), `GET /api/catalog/categories`
- Order endpoints: `POST /api/orders` (create), `GET /api/orders` (list, filter by accountId), `GET /api/orders/{id}` (detail)
- Account endpoints: `GET /api/accounts` (list), `GET /api/accounts/{id}` (detail)
- CORS must be configured on gateway-service to allow frontend origin

</code_context>

<specifics>
## Specific Ideas

No specific requirements beyond the UI-SPEC — open to standard Angular 17+ patterns and best practices recommended by the researcher.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 2-Angular Frontend*
*Context gathered: 2026-06-21*
