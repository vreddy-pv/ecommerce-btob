# Phase 2: Angular Frontend - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-21
**Phase:** 02-angular-frontend
**Areas discussed:** Project structure & Angular version

---

## Project structure & Angular version

### Question 1: Component Architecture

| Option | Description | Selected |
|--------|-------------|----------|
| Standalone components (Recommended) | Angular 17 default — no NgModules, simpler imports, lazy-loadable by default, less boilerplate. Modern Angular best practice. | ✓ |
| Traditional NgModules | Shared/feature/core module pattern. More boilerplate but familiar to teams with Angular <14 experience. | |

**User's choice:** Standalone components (Recommended)
**Notes:** None

---

### Question 2: Folder Structure

| Option | Description | Selected |
|--------|-------------|----------|
| Feature folders (Recommended) | Group by feature: features/catalog/, features/orders/, features/auth/, core/services/, shared/components/. Each feature is self-contained — easier to maintain and lazy-load. | ✓ |
| Layered folders | Group by type: components/, services/, models/, guards/. Simpler but features spread across folders — harder to lazy-load and maintain at scale. | |

**User's choice:** Feature folders (Recommended)
**Notes:** None

---

### Question 3: Frontend Location

| Option | Description | Selected |
|--------|-------------|----------|
| frontend/ subdirectory (Recommended) | Create frontend/ inside the existing project root. Keeps everything in one repo. Docker Compose can build and serve it. Backend stays in account-service/, catalog-service/, etc. | ✓ |
| Separate repository | New git repo for frontend. Cleaner separation but requires separate CI/CD, harder to coordinate API changes. | |

**User's choice:** frontend/ subdirectory (Recommended)
**Notes:** None

---

### Question 4: Serving Strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Dev proxy + Nginx prod (Recommended) | Dev: Angular dev server with proxy config to forward /api/* to gateway on port 8080 (avoids CORS). Prod: Build static files, serve via Nginx in Docker, reverse proxy /api/* to gateway. | |
| Backend CORS + static serving | Configure CORS on Spring Boot gateway, serve Angular static files from gateway-service. Simpler Docker setup but couples frontend to backend. | ✓ |
| You decide | Let the researcher/planner choose the best serving strategy based on the existing Docker Compose setup. | |

**User's choice:** Backend CORS + static serving
**Notes:** User preferred simpler Docker setup over decoupled Nginx approach.

---

## the agent's Discretion

- State management approach (Signals vs NgRx vs RxJS) — not discussed, researcher/planner to decide
- Auth & API integration (JWT storage, interceptor, token refresh) — not discussed, researcher/planner to decide
- Routing structure & auth guards — not discussed, planner to design
- Angular CLI version, build tooling, testing framework — researcher to recommend

## Deferred Ideas

None — discussion stayed within phase scope
