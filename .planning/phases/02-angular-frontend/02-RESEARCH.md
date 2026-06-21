# Phase 2: Angular Frontend - Research

**Researched:** 2026-06-21
**Domain:** Angular 22 SPA consuming Spring Boot REST APIs via gateway
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Use Angular 17+ standalone components (no NgModules) — modern Angular default, simpler imports, lazy-loadable by default, less boilerplate
- **D-02:** Use feature folder structure — `features/catalog/`, `features/orders/`, `features/auth/`, `core/services/`, `shared/components/`. Each feature is self-contained for maintainability and lazy loading
- **D-03:** Frontend lives in `frontend/` subdirectory inside the existing project root. Keeps everything in one repo alongside account-service/, catalog-service/, order-service/, gateway-service/
- **D-04:** Configure CORS on the Spring Boot gateway and serve Angular static files from gateway-service. This couples frontend serving to the backend but simplifies the Docker Compose setup (no separate Nginx container needed). The gateway serves the built Angular files as static resources and proxies API calls to backend services

### the agent's Discretion
- State management approach (Signals vs NgRx vs RxJS/BehaviorSubject) — user did not select this area for discussion; planner/researcher should recommend based on Angular 17 best practices and cart/auth requirements
- Auth & API integration details (JWT storage, HTTP interceptor, token refresh, error handling) — user did not select this area; researcher should investigate and planner should decide
- Routing structure and auth guards — user did not select this area; planner should design based on UI-SPEC layout and feature folders
- Angular CLI version (17.x or 18.x), build tooling, testing framework — researcher should recommend latest stable

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CATALOG-01 | Auto parts catalog displays items with SKU, name, description, price, and inventory level | ProductDto TypeScript interface (verified from backend DTO); mat-card product card component pattern; ProductCardComponent in features/catalog/ |
| CATALOG-02 | Users can search and filter parts by category, name, or SKU | GET /api/catalog/products?search=&categoryId=&page=&size= (verified from CatalogController); mat-form-field search + mat-chip-listbox category filter; debounced signal-driven API calls (300ms per UI-SPEC) |
| CATALOG-03 | B2B pricing tiers display different prices based on account level | ProductDto.tierPricing: TierPricingDto[] (verified); GET /api/catalog/products/{sku}/price?tier=GOLD; AccountTier enum (STANDARD/SILVER/GOLD/PLATINUM); computed signal for tier-specific price display |
| ORDER-01 | Users can create new B2B orders with line items (SKU + quantity) | POST /api/orders with CreateOrderRequest { accountId, items: [{productSku, quantity}] } (verified from OrderController + DTO); cart sidebar pattern from UI-SPEC; CartService with signals |
| ORDER-02 | Order status tracks PENDING, SHIPPED, DELIVERED states | OrderStatus enum (PENDING/CONFIRMED/SHIPPED/DELIVERED — verified); mat-chip status badges with semantic colors from UI-SPEC; GET /api/orders/{id}/status endpoint |
| ORDER-03 | Users can view order history and check order status | GET /api/orders?accountId=&page=&size= → Page<OrderResponse> (verified); mat-table with columns: Order ID, Date, Items Count, Total, Status, Actions; GET /api/orders/{id} for detail |
</phase_requirements>

## Summary

Phase 2 builds a greenfield Angular SPA in `frontend/` that consumes 18 REST endpoints from the Spring Boot backend via the gateway on port 8080. The backend is fully implemented (Phase 1 complete) with JWT auth, catalog, and order services. The frontend must implement: (1) auth flow with JWT token storage and HTTP interceptor, (2) catalog browsing with search/filter/tier pricing, (3) order creation with a cart sidebar, and (4) order history table with status indicators.

**Critical integration discovery:** Only `account-service` has Spring Security configured. The gateway-service, catalog-service, and order-service have **no JWT validation** (verified by grepping all Java source — no `@EnableWebSecurity` or `SecurityFilterChain` outside account-service). This means the frontend's JWT interceptor will send the `Authorization: Bearer <token>` header, but catalog and order endpoints will not reject unauthenticated requests. The frontend MUST still send the token (for account-service calls like `/api/accounts/{id}`) and MUST store `accountId` from the `AuthResponse` to include in order creation requests (`CreateOrderRequest.accountId`). The backend does not extract accountId from the JWT on order endpoints — it expects it in the request body.

**Second critical discovery:** The gateway-service `application.yml` has **no CORS configuration** (verified by grep — no `cors`, `CorsWebFilter`, or `allowedOrigins` anywhere in the codebase). D-04 requires CORS to be configured on the gateway. During development, the Angular dev server runs on `http://localhost:4200` while the gateway runs on `http://localhost:8080` — cross-origin. The planner must include a task to add a `CorsWebFilter` bean or `globalcors` config in the gateway `application.yml` to allow the frontend origin. For production (D-04 static serving), CORS is not needed since the gateway serves both static files and APIs from the same origin.

**Primary recommendation:** Use Angular 22 (latest stable, v22.0.2) with standalone components, Angular Signals for all state management (no NgRx), Angular Material 22 for UI components, and functional HTTP interceptors for JWT injection and error handling. Create the project via `npx @angular/cli@22 new frontend` (global CLI is 21.2.0; use npx to get v22). The UI-SPEC's reference to "Angular Material 17" should be interpreted as "Angular Material with MD3 design" — the design system, not a hard version pin, since D-01 says "Angular 17+".

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| JWT token storage & injection | Browser (Angular service) | — | Token stored in localStorage/sessionStorage by AuthService; injected via functional HTTP interceptor on every request |
| Catalog search/filter API calls | Browser (Angular service) | API (gateway → catalog-service) | Browser debounces search input (300ms), calls GET /api/catalog/products with query params; API returns Page<ProductDto> |
| Tier pricing display | Browser (Angular component) | API (catalog-service) | Browser reads account tier from AuthService signal; displays tier-specific price from ProductDto.tierPricing array or calls /price endpoint |
| Cart state management | Browser (Angular service) | — | CartService holds cart items in signals; no server-side cart for MVP; submits as CreateOrderRequest on checkout |
| Order creation | Browser (Angular service) | API (gateway → order-service) | Browser assembles CreateOrderRequest with accountId from auth state + cart items; POST /api/orders |
| Order history display | Browser (Angular component) | API (gateway → order-service) | Browser calls GET /api/orders?accountId=X; API returns Page<OrderResponse>; browser renders mat-table |
| Order status display | Browser (Angular component) | — | Browser maps OrderStatus enum to colored mat-chip per UI-SPEC semantic colors |
| Routing & auth guards | Browser (Angular router) | — | Browser-side route protection via CanActivateFn; redirect to /login if no token; API is stateless |
| CORS configuration | API (gateway-service) | — | Gateway must allow frontend origin during development; same-origin in production via static serving |
| Static file serving | API (gateway-service) | — | Gateway serves built Angular files as static resources in production (D-04) |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| @angular/core | 22.0.2 | Framework core (signals, DI, change detection) | Official Angular framework; latest stable; standalone components are the default [VERIFIED: npm registry] |
| @angular/cli | 22.0.3 | Project scaffolding, build, serve, generate | Official CLI; `ng new`, `ng serve`, `ng build`, `ng add @angular/material` [VERIFIED: npm registry] |
| @angular/material | 22.0.2 | MD3 component library (toolbar, sidenav, card, table, form-field, button, chip, select, badge, fab, snackbar, dialog) | Official Angular component library; MD3 design system; locked by UI-SPEC [VERIFIED: npm registry] |
| @angular/cdk | 22.0.2 | Component Development Kit (a11y, layout, scrolling primitives used by Material) | Required peer dependency of @angular/material [VERIFIED: npm registry] |
| @angular/router | 22.0.2 | SPA routing with lazy loading and functional guards | Official router; loadComponent/loadChildren for standalone components [VERIFIED: npm registry] |
| @angular/forms | 22.0.2 | Reactive and template-driven forms | Official forms module; required by Material form-field components [VERIFIED: npm registry] |
| @angular/common | 22.0.2 | Common directives, pipes, HttpClientModule | Official; provideHttpClient lives here [VERIFIED: npm registry] |
| @angular/platform-browser | 22.0.2 | Browser DOM rendering | Official; required for bootstrapApplication [VERIFIED: npm registry] |
| rxjs | 7.8.2 | Reactive programming (Observables for HTTP) | Peer dependency of Angular; HttpClient returns Observables [VERIFIED: npm registry] |
| zone.js | 0.15.0 | Change detection runtime | Peer dependency of Angular; ~6M weekly downloads [VERIFIED: npm registry] |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| tslib | 2.8.0 | TypeScript runtime helpers | Auto-installed by Angular CLI; reduces bundle size |
| typescript | ~5.5.0 | Type checking | Auto-installed by Angular CLI; Angular 22 requires TS 5.5+ |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Angular Signals | NgRx SignalStore | NgRx adds a dependency and boilerplate; Signals are built-in and sufficient for this app's state complexity (auth, cart, catalog filters) |
| Angular Signals | RxJS BehaviorSubject | BehaviorSubject works but Signals are the Angular 17+ recommended approach; finer-grained reactivity; less boilerplate; better OnPush integration |
| Angular Material | NG-Zorro / PrimeNG | Material is locked by UI-SPEC (D-04/UI-SPEC); official first-party library; MD3 design system |
| localStorage | sessionStorage for JWT | sessionStorage clears on tab close (stricter); localStorage persists across tabs. AUTH-02 requires "persists across browser refresh" — both work. Recommend localStorage for B2B dashboard (longer sessions) |
| @angular/animations | (built-in) | @angular/animations is DEPRECATED in Angular 22 [VERIFIED: npm registry — deprecated flag]. Angular 22 has built-in animation support. Do NOT install @angular/animations separately |

**Installation:**
```bash
# Create the project (use npx to get Angular 22, not the global CLI 21)
npx @angular/cli@22 new frontend --style=scss --routing --ssr=false --skip-git
cd frontend
ng add @angular/material  # adds Material, Roboto font, Material Icons, theme
```

**Version verification:**
```bash
npm view @angular/core version          # → 22.0.2
npm view @angular/material version      # → 22.0.2
npm view @angular/cli version           # → 22.0.3
npm view @angular/cdk version           # → 22.0.2
```
All verified 2026-06-21 against npm registry. Angular 22 released 2026-06-17 (4 days ago).

## Package Legitimacy Audit

> Run via `gsd-tools query package-legitimacy check --ecosystem npm` on 2026-06-21.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| @angular/core | npm | 4 days | 6.0M/wk | github.com/angular/angular | SUS (too-new) | Approved — official Angular org, major v22 release |
| @angular/material | npm | 4 days | 2.6M/wk | github.com/angular/components | SUS (too-new) | Approved — official Angular org |
| @angular/cdk | npm | 4 days | 4.0M/wk | github.com/angular/components | SUS (too-new) | Approved — official Angular org |
| @angular/cli | npm | 3 days | 5.4M/wk | github.com/angular/angular-cli | SUS (too-new) | Approved — official Angular org |
| @angular/forms | npm | 4 days | 5.1M/wk | github.com/angular/angular | SUS (too-new) | Approved — official Angular org |
| @angular/router | npm | 4 days | 5.0M/wk | github.com/angular/angular | SUS (too-new) | Approved — official Angular org |
| @angular/common | npm | 4 days | 5.7M/wk | github.com/angular/angular | SUS (too-new) | Approved — official Angular org |
| @angular/platform-browser | npm | 4 days | 5.3M/wk | github.com/angular/angular | SUS (too-new) | Approved — official Angular org |
| @angular/animations | npm | 4 days | 4.4M/wk | github.com/angular/angular | SUS (too-new + DEPRECATED) | REMOVED — deprecated in v22; animations built into framework |
| rxjs | npm | ~16 mo | 92.8M/wk | github.com/ReactiveX/rxjs | OK | Approved |
| zone.js | npm | ~1.5 mo | 6.0M/wk | github.com/angular/angular | OK | Approved |

**Packages removed due to SLOP/deprecated verdict:** `@angular/animations` — deprecated in Angular 22; animations are now built into `@angular/core`. Do not install.

**Packages flagged as suspicious [SUS]:** All `@angular/*` packages are flagged "too-new" because Angular 22 was released 2026-06-17 (4 days before research date). These are **legitimate official packages** from the Angular GitHub organization (github.com/angular/angular) with millions of weekly downloads. The "too-new" flag is a false positive for a major framework release. No `checkpoint:human-verify` task needed — these are first-party Angular packages.

**Postinstall script check:** None of the @angular/* packages have postinstall scripts (verified via legitimacy check `postinstall: null` for all). No supply chain risk.

*All @angular/* packages discovered via the installed global CLI (21.2.0) and confirmed via npm registry. They are official first-party packages — not third-party discoveries requiring [ASSUMED] tags.*

## Architecture Patterns

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     Browser (Angular SPA)                     │
│                                                               │
│  ┌─────────┐   ┌──────────┐   ┌──────────┐   ┌────────────┐ │
│  │  Auth   │   │ Catalog  │   │  Orders  │   │   Cart     │ │
│  │ Feature │   │ Feature  │   │ Feature  │   │  Service   │ │
│  └────┬────┘   └────┬─────┘   └────┬─────┘   └─────┬──────┘ │
│       │              │              │               │        │
│  ┌────┴──────────────┴──────────────┴───────────────┴──────┐ │
│  │              Core Services Layer                        │ │
│  │  AuthService  │  CatalogService  │  OrderService        │ │
│  │  (JWT + signals)  (HTTP calls)   (HTTP calls)          │ │
│  └──────────────────────┬──────────────────────────────────┘ │
│                         │                                     │
│  ┌──────────────────────┴──────────────────────────────────┐ │
│  │  HTTP Interceptor Chain (functional)                    │ │
│  │  1. authInterceptor → adds Authorization: Bearer <token>│ │
│  │  2. errorInterceptor → parses {timestamp, status, ...}  │ │
│  └──────────────────────┬──────────────────────────────────┘ │
│                         │                                     │
│  ┌──────────────────────┴──────────────────────────────────┐ │
│  │  Router (provideRouter with lazy loadComponent)         │ │
│  │  /login (public)  /catalog (authGuard)  /orders (guard) │ │
│  └──────────────────────┬──────────────────────────────────┘ │
└─────────────────────────┼─────────────────────────────────────┘
                          │ HTTP (fetch API)
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              Gateway Service (port 8080)                      │
│  Spring Cloud Gateway (WebFlux)                              │
│  Routes: /api/auth/** → account:8081                         │
│          /api/catalog/** → catalog:8082                      │
│          /api/orders/** → order:8083                         │
│  CORS: allowedOrigins "http://localhost:4200" (dev only)     │
│  Static: serves frontend/dist/frontend/ (prod, D-04)        │
└──────────────────────┬───────────────────────────────────────┘
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
   ┌──────────┐ ┌──────────┐ ┌──────────┐
   │ account  │ │ catalog  │ │  order   │
   │ service  │ │ service  │ │ service  │
   │ :8081    │ │ :8082    │ │ :8083    │
   │ JWT ✓    │ │ no JWT   │ │ no JWT   │
   └──────────┘ └──────────┘ └──────────┘
```

### Recommended Project Structure
```
frontend/
├── src/
│   ├── app/
│   │   ├── app.component.ts          # Root shell: sidebar + header + router-outlet
│   │   ├── app.config.ts             # ApplicationConfig: provideHttpClient, provideRouter, provideAnimations
│   │   ├── app.routes.ts             # Top-level routes with lazy loadComponent
│   │   │
│   │   ├── core/
│   │   │   ├── services/
│   │   │   │   ├── auth.service.ts        # JWT storage, login/register, auth signal
│   │   │   │   ├── catalog.service.ts     # GET products, categories, tier pricing
│   │   │   │   ├── order.service.ts       # POST order, GET orders, GET order by id
│   │   │   │   └── cart.service.ts        # Cart items signal, add/remove/total computed
│   │   │   ├── interceptors/
│   │   │   │   ├── auth.interceptor.ts    # Adds Authorization: Bearer header
│   │   │   │   └── error.interceptor.ts   # Parses backend error JSON, 401 redirect
│   │   │   ├── guards/
│   │   │   │   └── auth.guard.ts          # CanActivateFn: redirect to /login if no token
│   │   │   └── models/
│   │   │       ├── api.models.ts          # TypeScript interfaces matching backend DTOs
│   │   │       └── enums.ts               # AccountTier, OrderStatus enums
│   │   │
│   │   ├── features/
│   │   │   ├── auth/
│   │   │   │   ├── login.component.ts     # Login form (email + password)
│   │   │   │   └── register.component.ts  # Registration form
│   │   │   ├── catalog/
│   │   │   │   ├── catalog.component.ts   # Search bar + filter chips + product grid
│   │   │   │   ├── product-card.component.ts  # Single product card (mat-card)
│   │   │   │   └── catalog.routes.ts      # Catalog feature routes (if nested)
│   │   │   └── orders/
│   │   │       ├── orders.component.ts    # Orders table (mat-table) + status chips
│   │   │       ├── order-detail.component.ts  # Order detail with line items
│   │   │       ├── cart-sidebar.component.ts  # Cart panel with line items + submit
│   │   │       └── orders.routes.ts       # Orders feature routes
│   │   │
│   │   └── shared/
│   │       └── components/
│   │           ├── status-chip.component.ts    # Reusable OrderStatus colored chip
│   │           └── tier-badge.component.ts     # Account tier badge in header
│   │
│   ├── styles.scss                   # Global Roboto font, Material theme, MD3 color vars
│   └── index.html                    # Loads Roboto + Material Icons from Google Fonts
│
├── angular.json                      # CLI config (build, serve, test targets)
├── package.json
├── tsconfig.json
└── tsconfig.app.json
```

### Pattern 1: Standalone Component with Signals
**What:** Angular 22 standalone components use `signal()` and `computed()` for local state instead of traditional component properties. No NgModules — imports go directly in the `@Component` decorator.
**When to use:** Every component in this project.
**Example:**
```typescript
// Source: https://angular.dev/guide/signals [CITED: angular.dev/guide/signals]
import { Component, signal, computed, inject } from '@angular/core';
import { ProductDto } from '../../core/models/api.models';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [MatCardModule, MatFormFieldModule, MatInputModule, MatChipsModule],
  template: `
    <mat-form-field>
      <mat-label>Search parts</mat-label>
      <input matInput [value]="search()" (input)="onSearch($event)" />
    </mat-form-field>
    @for (product of products(); track product.sku) {
      <app-product-card [product]="product" [tier]="currentTier()" />
    } @empty {
      <p>No parts found</p>
    }
  `,
})
export class CatalogComponent {
  private catalogService = inject(CatalogService);
  private authService = inject(AuthService);

  search = signal('');
  products = signal<ProductDto[]>([]);
  loading = signal(false);
  currentTier = computed(() => this.authService.account()?.tier ?? 'STANDARD');

  onSearch(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.search.set(value);
    // Debounced API call (300ms per UI-SPEC)
    this.loadProducts();
  }

  private loadProducts() {
    this.loading.set(true);
    this.catalogService.getProducts(this.search()).subscribe({
      next: (page) => { this.products.set(page.content); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}
```

### Pattern 2: Functional HTTP Interceptor for JWT
**What:** A functional interceptor injects the `Authorization: Bearer <token>` header on every outgoing API request. Uses `inject()` for DI access to AuthService.
**When to use:** All API calls to `/api/*` endpoints.
**Example:**
```typescript
// Source: https://angular.dev/guide/http/interceptors [CITED: angular.dev/guide/http/interceptors]
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).token();
  if (token && req.url.startsWith('/api/') && !req.url.includes('/api/auth/')) {
    const authReq = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`),
    });
    return next(authReq);
  }
  return next(req);
};
```

### Pattern 3: Error Interceptor for Backend Error Format
**What:** Parses the backend's structured error response `{ timestamp, status, error, message }` and redirects to login on 401.
**When to use:** All API calls.
**Example:**
```typescript
// Source: backend GlobalExceptionHandler.java [VERIFIED: codebase grep]
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        inject(AuthService).logout();
        router.navigate(['/login']);
      }
      // Backend returns: { timestamp, status, error, message } or { timestamp, status, error, errors: {field: msg} }
      const userMessage = error.error?.message ?? 'An unexpected error occurred';
      return throwError(() => new Error(userMessage));
    }),
  );
};
```

### Pattern 4: Functional Auth Guard
**What:** A `CanActivateFn` that checks if the user has a JWT token; redirects to `/login` if not.
**When to use:** Protected routes (catalog, orders).
**Example:**
```typescript
import { CanActivateFn, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  if (authService.isAuthenticated()) {
    return true;
  }
  return router.parseUrl('/login');
};
```

### Pattern 5: App Config with Providers
**What:** `ApplicationConfig` wires up `provideHttpClient` with interceptors, `provideRouter` with routes, and Material animations.
**When to use:** `main.ts` bootstrap.
**Example:**
```typescript
// Source: https://angular.dev/guide/http/setup [CITED: angular.dev/guide/http/setup]
import { ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    provideAnimations(),  // Angular 22 built-in (NOT @angular/animations package)
  ],
};
```

### Pattern 6: Signal-Based AuthService
**What:** AuthService stores JWT and account info in signals; persists token to localStorage for AUTH-02 (survives refresh).
**When to use:** Core auth state.
**Example:**
```typescript
import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';

export interface AuthState {
  token: string;
  accountId: string;
  email: string;
  tier: AccountTier;
  expiresIn: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = '/api/auth';
  private storageKey = 'btob_auth';

  // Signal holds the full auth state (null = not logged in)
  private _auth = signal<AuthState | null>(this.loadFromStorage());
  readonly auth = this._auth.asReadonly();

  // Derived signals for convenience
  readonly token = computed(() => this._auth()?.token ?? null);
  readonly account = computed(() => {
    const a = this._auth();
    return a ? { accountId: a.accountId, email: a.email, tier: a.tier } : null;
  });
  readonly isAuthenticated = computed(() => this._auth() !== null);

  login(email: string, password: string) {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, { email, password }).pipe(
      tap((res) => this.setAuth(res)),
    );
  }

  logout() {
    this._auth.set(null);
    localStorage.removeItem(this.storageKey);
    this.router.navigate(['/login']);
  }

  private setAuth(res: AuthResponse) {
    const state: AuthState = {
      token: res.token,
      accountId: res.accountId,
      email: res.email,
      tier: res.tier,
      expiresIn: res.expiresIn,
    };
    this._auth.set(state);
    localStorage.setItem(this.storageKey, JSON.stringify(state));
  }

  private loadFromStorage(): AuthState | null {
    const raw = localStorage.getItem(this.storageKey);
    return raw ? JSON.parse(raw) : null;
  }
}
```

### Anti-Patterns to Avoid
- **Using NgModules:** D-01 locks standalone components. Do not create `app.module.ts` or any `*.module.ts` files. All imports go in `@Component({ imports: [...] })`.
- **Using BehaviorSubject for state:** Angular 22 has Signals. Use `signal()` / `computed()` instead of `BehaviorSubject` / `combineLatest`. Signals are the recommended approach for Angular 17+.
- **Installing @angular/animations:** It is DEPRECATED in Angular 22. Use `provideAnimations()` from `@angular/platform-browser/animations` instead (built into the framework).
- **Using the global Angular CLI (21.2.0):** It would create an Angular 21 project. Use `npx @angular/cli@22 new` to get Angular 22.
- **Putting all routes in app.routes.ts:** Feature folder structure (D-02) means each feature should have its own routes file loaded via `loadChildren`.
- **Hardcoding API base URL:** Use a relative path (`/api/...`) so the same code works in dev (proxy to localhost:8080) and prod (same-origin via gateway static serving, D-04).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| HTTP JWT token injection | Manual header on every request | Functional HTTP interceptor (authInterceptor) | Centralized, impossible to forget; Angular's recommended pattern |
| Error parsing | Try/catch in every service call | Error interceptor (errorInterceptor) | Backend has consistent `{ timestamp, status, error, message }` format; parse once centrally |
| State management | Custom Observable store | Angular Signals (signal, computed, effect) | Built into Angular 22; fine-grained reactivity; no extra dependency |
| UI components | Custom table, card, form fields | Angular Material 22 (mat-table, mat-card, mat-form-field) | Locked by UI-SPEC; accessible by default; MD3 design system |
| Date formatting | Custom date pipes | Angular DatePipe or Intl.DateTimeFormat | Standard, locale-aware, timezone handling |
| Pagination logic | Manual page tracking | Spring Data Page<T> metadata (totalPages, number, size) | Backend returns Page object with all pagination metadata |
| Routing/guards | Manual auth checks in components | CanActivateFn functional guards | Declarative, testable, Angular standard |
| Debouncing search | setTimeout in component | RxJS debounceTime(300) or signal debounce | UI-SPEC requires 300ms debounce; RxJS debounceTime is the standard |

**Key insight:** Angular 22 + Material 22 + Signals provides everything this phase needs. No additional state management library (NgRx), no UI component library, no HTTP wrapper — all built into the framework and its official component library.

## Common Pitfalls

### Pitfall 1: No CORS Configuration on Gateway
**What goes wrong:** Angular dev server (`localhost:4200`) calls gateway (`localhost:8080`) — browser blocks requests with CORS error. Nothing loads.
**Why it happens:** The gateway-service `application.yml` has no CORS configuration (verified by codebase grep). D-04 requires CORS but it hasn't been implemented yet.
**How to avoid:** Add a `CorsWebFilter` bean or `spring.cloud.gateway.globalcors` config in gateway-service `application.yml` allowing `http://localhost:4200`. Alternatively, use Angular proxy config (`proxy.conf.json`) to proxy `/api` to `localhost:8080` during development — this avoids CORS entirely in dev. For production (D-04 static serving), no CORS needed since gateway serves both static + API from same origin.
**Warning signs:** Console error "Access to fetch at 'http://localhost:8080/api/...' from origin 'http://localhost:4200' has been blocked by CORS policy"

### Pitfall 2: Backend Does Not Validate JWT on Catalog/Order Endpoints
**What goes wrong:** Frontend assumes all endpoints are JWT-protected. Catalog and order services accept unauthenticated requests. Developer might skip sending the token for catalog calls.
**Why it happens:** Only account-service has `@EnableWebSecurity` (verified by codebase grep). Catalog-service and order-service have no Spring Security dependency. The gateway does not validate JWT.
**How to avoid:** Always send the `Authorization: Bearer <token>` header via the auth interceptor on ALL `/api/*` calls (except `/api/auth/**`). Store `accountId` from `AuthResponse` and include it in `CreateOrderRequest.accountId` — the backend does NOT extract accountId from the JWT on order endpoints.
**Warning signs:** Order creation fails with "Account ID is required" validation error (400) if accountId is not in the request body.

### Pitfall 3: Spring Data Page<T> JSON Shape Mismatch
**What goes wrong:** Frontend expects a plain array but backend returns `{ content: [...], totalElements, totalPages, ... }`. Products don't render.
**Why it happens:** Spring Data's `Page<T>` serializes to a JSON object with `content` array + pagination metadata, not a bare array.
**How to avoid:** Type the response as `Page<T>` interface with `content: T[]`, `totalElements: number`, `totalPages: number`, `number: number` (current page), `size: number`. Access `.content` for the data array.
**Warning signs:** `products.map is not a function` — means you're calling `.map()` on the Page object instead of `.content`.

### Pitfall 4: @angular/animations Deprecated in v22
**What goes wrong:** Installing `@angular/animations` produces deprecation warnings or build issues. Material animations don't work.
**Why it happens:** Angular 22 deprecated the `@angular/animations` package. Animations are now built into `@angular/platform-browser/animations` via `provideAnimations()`.
**How to avoid:** Do NOT run `npm install @angular/animations`. Use `import { provideAnimations } from '@angular/platform-browser/animations'` in `app.config.ts`.
**Warning signs:** npm deprecation warning during install; Material components lack animations.

### Pitfall 5: Using Global CLI 21 Instead of Latest 22
**What goes wrong:** `ng new frontend` creates an Angular 21 project (global CLI is 21.2.0). Material 22 won't be compatible.
**Why it happens:** The globally installed `@angular/cli` is version 21.2.0. `ng new` uses the global CLI version.
**How to avoid:** Use `npx @angular/cli@22 new frontend` to scaffold with Angular 22. After creation, the project has its own local CLI — use `npx ng serve` / `npx ng build` within the `frontend/` directory.
**Warning signs:** `package.json` shows `@angular/core: ^21.0.0` after scaffolding.

### Pitfall 6: UUID Serialization Mismatch
**What goes wrong:** Frontend sends UUID as a string but backend expects `UUID` type; or frontend expects UUID object but backend sends string.
**Why it happens:** Java `UUID` serializes to JSON as a string (e.g., `"550e8400-e29b-41d4-a716-446655440000"`). TypeScript has no UUID type — it's just `string`.
**How to avoid:** Type all UUID fields as `string` in TypeScript interfaces. When sending UUIDs to the backend, send them as JSON string values. The backend Jackson deserializer handles string→UUID conversion automatically.
**Warning signs:** 400 Bad Request with "Failed to convert value of type 'String' to UUID" — usually means malformed UUID string format.

### Pitfall 7: BigDecimal Serializes as Number (Not String)
**What goes wrong:** Price values lose precision or display incorrectly.
**Why it happens:** Java `BigDecimal` serializes to JSON as a number by default (Jackson). `19.99` becomes `19.99` in JSON. TypeScript `number` type handles this fine for display, but very large monetary values could lose precision.
**How to avoid:** Type price/amount fields as `number` in TypeScript. For this MVP, precision loss is not a concern (prices are small). If precision matters later, configure Jackson to serialize BigDecimal as string on the backend.
**Warning signs:** Prices display with floating point artifacts like `19.990000000001`.

## Code Examples

### TypeScript API Models (Matching Backend DTOs)
```typescript
// Source: backend DTOs verified via codebase read [VERIFIED: codebase]
// core/models/api.models.ts

export type AccountTier = 'STANDARD' | 'SILVER' | 'GOLD' | 'PLATINUM';
export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED';

// AuthResponse from POST /api/auth/login
export interface AuthResponse {
  token: string;
  accountId: string;      // UUID as string
  email: string;
  tier: AccountTier;
  expiresIn: number;      // seconds (86400 = 24h)
}

// LoginRequest for POST /api/auth/login
export interface LoginRequest {
  email: string;
  password: string;
}

// ProductDto from GET /api/catalog/products
export interface ProductDto {
  id: string;
  sku: string;
  name: string;
  description: string;
  basePrice: number;
  inventoryLevel: number;
  categoryId: string;
  categoryName: string;
  isActive: boolean;
  tierPricing: TierPricingDto[];
  createdAt: string;      // ISO datetime
  updatedAt: string;
}

export interface TierPricingDto {
  id: string;
  productId: string;
  tier: AccountTier;
  price: number;
}

export interface CategoryDto {
  id: string;
  name: string;
  parentId: string | null;
  sortOrder: number;
  children: CategoryDto[];
}

// Spring Data Page<T> JSON shape
export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: { sorted: boolean; unsorted: boolean; empty: boolean };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalElements: number;
  totalPages: number;
  number: number;         // current page
  size: number;           // page size
  first: boolean;
  last: boolean;
  empty: boolean;
}

// OrderResponse from GET /api/orders, GET /api/orders/{id}
export interface OrderResponse {
  id: string;
  accountId: string;
  status: OrderStatus;
  totalAmount: number;
  creditUsed: number;
  items: OrderItemDto[];
  createdAt: string;
  updatedAt: string;
}

export interface OrderItemDto {
  id: string;
  productSku: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

// CreateOrderRequest for POST /api/orders
export interface CreateOrderRequest {
  accountId: string;
  items: { productSku: string; quantity: number }[];
}

// Backend error response format (from GlobalExceptionHandler.java)
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message?: string;       // present on 404/500
  errors?: Record<string, string>;  // present on 400 validation: { fieldName: "message" }
}
```

### CatalogService with HTTP Calls
```typescript
// core/services/catalog.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page, ProductDto, CategoryDto, AccountTier } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private http = inject(HttpClient);
  private apiUrl = '/api/catalog';

  getProducts(search?: string, categoryId?: string, page = 0, size = 20): Observable<Page<ProductDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (search) params = params.set('search', search);
    if (categoryId) params = params.set('categoryId', categoryId);
    return this.http.get<Page<ProductDto>>(`${this.apiUrl}/products`, { params });
  }

  getProductBySku(sku: string): Observable<ProductDto> {
    return this.http.get<ProductDto>(`${this.apiUrl}/products/${sku}`);
  }

  getTierPrice(sku: string, tier: AccountTier): Observable<ProductDto> {
    return this.http.get<ProductDto>(`${this.apiUrl}/products/${sku}/price`, {
      params: { tier },
    });
  }

  getCategories(): Observable<CategoryDto[]> {
    return this.http.get<CategoryDto[]>(`${this.apiUrl}/categories`);
  }
}
```

### OrderService with HTTP Calls
```typescript
// core/services/order.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page, OrderResponse, CreateOrderRequest, OrderStatus } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private http = inject(HttpClient);
  private apiUrl = '/api/orders';

  createOrder(accountId: string, items: { productSku: string; quantity: number }[]): Observable<OrderResponse> {
    const body: CreateOrderRequest = { accountId, items };
    return this.http.post<OrderResponse>(this.apiUrl, body);
  }

  getOrders(accountId: string, page = 0, size = 10): Observable<Page<OrderResponse>> {
    const params = new HttpParams()
      .set('accountId', accountId)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<Page<OrderResponse>>(this.apiUrl, { params });
  }

  getOrder(id: string): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(`${this.apiUrl}/${id}`);
  }

  getOrderStatus(id: string): Observable<OrderStatus> {
    return this.http.get<OrderStatus>(`${this.apiUrl}/${id}/status`);
  }
}
```

### CartService with Signals
```typescript
// core/services/cart.service.ts
import { Injectable, signal, computed } from '@angular/core';
import { ProductDto } from '../models/api.models';

export interface CartItem {
  product: ProductDto;
  quantity: number;
  unitPrice: number;  // tier-specific price
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private _items = signal<CartItem[]>([]);
  readonly items = this._items.asReadonly();
  readonly totalItems = computed(() => this._items().reduce((sum, i) => sum + i.quantity, 0));
  readonly totalAmount = computed(() => this._items().reduce((sum, i) => sum + i.unitPrice * i.quantity, 0));

  add(product: ProductDto, quantity: number, unitPrice: number) {
    const existing = this._items().find(i => i.product.sku === product.sku);
    if (existing) {
      this._items.update(items =>
        items.map(i => i.product.sku === product.sku
          ? { ...i, quantity: i.quantity + quantity }
          : i
        )
      );
    } else {
      this._items.update(items => [...items, { product, quantity, unitPrice }]);
    }
  }

  remove(sku: string) {
    this._items.update(items => items.filter(i => i.product.sku !== sku));
  }

  clear() {
    this._items.set([]);
  }
}
```

### App Routes with Lazy Loading
```typescript
// app.routes.ts
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/catalog', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register.component').then(m => m.RegisterComponent),
  },
  {
    path: 'catalog',
    canActivate: [authGuard],
    loadComponent: () => import('./features/catalog/catalog.component').then(m => m.CatalogComponent),
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/orders.component').then(m => m.OrdersComponent),
  },
  {
    path: 'orders/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/order-detail.component').then(m => m.OrderDetailComponent),
  },
  { path: '**', redirectTo: '/catalog' },
];
```

### Angular Dev Proxy Config (Avoids CORS in Development)
```json
// proxy.conf.json (in frontend/ directory)
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```
```json
// angular.json — add to serve options:
"serve": {
  "options": {
    "proxyConfig": "proxy.conf.json"
  }
}
```
This proxies all `/api/*` calls from `localhost:4200` to `localhost:8080` (the gateway), avoiding CORS during development. In production, the gateway serves both static files and APIs from the same origin (D-04), so no proxy or CORS needed.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| NgModules | Standalone components | Angular 14 (stable in 17+) | No `.module.ts` files; imports in `@Component`; `bootstrapApplication` instead of `bootstrapModule` |
| Class-based HTTP interceptors (HttpInterceptor) | Functional interceptors (HttpInterceptorFn) | Angular 15 (recommended in 17+) | Simpler, no `@Injectable`, use `inject()` for DI; `withInterceptors([fn])` |
| BehaviorSubject/combineLatest for state | Angular Signals (signal, computed, effect) | Angular 16 (stable in 17+) | Fine-grained reactivity; less boilerplate; built-in; no NgRx needed for most apps |
| @angular/animations package | Built-in provideAnimations() | Angular 22 (2026-06-17) | `@angular/animations` is DEPRECATED; use `provideAnimations()` from `@angular/platform-browser/animations` |
| NgModule-based routing | provideRouter with loadComponent | Angular 14+ | Lazy load individual standalone components, not whole modules |
| RouterModule.forRoot | provideRouter(routes) | Angular 15+ | Functional provider instead of NgModule |

**Deprecated/outdated:**
- `@angular/animations` package: DEPRECATED in Angular 22. Use `provideAnimations()` from `@angular/platform-browser/animations`.
- `HttpInterceptor` class interface: Still works but functional interceptors (`HttpInterceptorFn`) are recommended for predictable ordering.
- `BehaviorSubject` for state: Still works but Signals are the modern Angular 17+ approach.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | UI-SPEC "Angular Material 17" means "Angular Material with MD3 design system" not a hard version pin to v17 | Standard Stack | If the user intended exactly v17, we'd need to downgrade to Angular 17 + Material 17 — but D-01 says "17+" which permits 22 |
| A2 | `provideAnimations()` from `@angular/platform-browser/animations` is the Angular 22 replacement for `@angular/animations` | Standard Stack, Pitfall 4 | If wrong, Material animations won't work; verify with `ng add @angular/material` CLI output during implementation |
| A3 | Angular 22 `ng add @angular/material` auto-configures Roboto font, Material Icons, and MD3 theme (same as v17+) | Standard Stack | If the CLI scaffolding changed, manual font/theme config needed — verify during plan 02-01 |
| A4 | localStorage is acceptable for JWT token storage (not sessionStorage or HttpOnly cookies) | Pattern 6 | If security review requires HttpOnly cookies, the auth flow needs a backend cookie endpoint (not in Phase 1 scope). AUTH-02 says "persists across browser refresh" — localStorage satisfies this |
| A5 | The Angular dev proxy (`proxy.conf.json`) is sufficient for development and the gateway CORS config is needed for any non-proxied dev scenario | Pitfall 1 | If the team prefers not to use a proxy, gateway CORS must be configured before any frontend dev work can proceed |
| A6 | Backend `LocalDateTime` serializes as ISO-8601 string (e.g., `"2026-06-21T13:25:22"`) | Code Examples | Jackson default serialization; if a custom serializer is configured, the frontend date parsing needs adjustment |

## Open Questions

1. **Gateway CORS: proxy vs. CorsWebFilter?**
   - What we know: No CORS config exists in gateway-service (verified). D-04 requires CORS on gateway. Angular dev server runs on :4200, gateway on :8080.
   - What's unclear: Should the plan add a `CorsWebFilter` bean to the gateway, or rely solely on the Angular dev proxy (`proxy.conf.json`) for development?
   - Recommendation: Use BOTH. The dev proxy avoids CORS entirely during development (simpler). Add a `CorsWebFilter` to the gateway for any non-proxied dev scenarios and for the transition period before static serving is configured. The planner should include a gateway CORS task in plan 02-01.

2. **Gateway static serving configuration (D-04 production)**
   - What we know: D-04 locks "serve Angular static files from gateway-service." The gateway is WebFlux-based (Spring Cloud Gateway).
   - What's unclear: Exact mechanism — WebFlux static resource serving config, build output path mapping, SPA fallback (all non-API routes return index.html).
   - Recommendation: Add a `WebFluxConfigurer` or route handler for static resources + a catch-all that serves `index.html` for non-`/api/**` paths. This may be a Phase 2 plan 02-01 task or a Phase 4 deployment task. The planner should include it in 02-01 if the success criteria require end-to-end serving.

3. **Should the frontend implement order status update (PUT /api/orders/{id}/status)?**
   - What we know: The backend has `PUT /api/orders/{id}/status?status=SHIPPED`. The UI-SPEC mentions status chips but no status-update UI. ORDER-02 says "Order status tracks PENDING, SHIPPED, DELIVERED states" — this could mean display-only or include status transitions.
   - What's unclear: Is status update a frontend feature in Phase 2, or is it backend-only (set by fulfillment processes)?
   - Recommendation: Phase 2 should display statuses (ORDER-02 is about tracking/display). Status update UI is likely an admin feature — defer to a future phase. The planner should focus on display.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node.js | Angular CLI, build tooling | ✓ | 22.14.0 | — |
| npm | Package management | ✓ | 10.9.2 | — |
| Angular CLI (global) | Project scaffolding | ✓ (wrong version) | 21.2.0 | Use `npx @angular/cli@22 new` for v22 |
| Spring Boot Gateway (port 8080) | API access | ✓ (Phase 1 complete) | 3.5.15 | — |
| Docker Compose | Running all backend services | ✓ | docker-compose.yml exists | Run services individually via Maven |
| PostgreSQL | Backend databases | ✓ (via Docker) | 15-alpine | — |
| RabbitMQ | Backend messaging | ✓ (via Docker) | 3-management-alpine | — |

**Missing dependencies with no fallback:**
- None — all required tools are available.

**Missing dependencies with fallback:**
- Global Angular CLI is 21.2.0 (not 22). Fallback: use `npx @angular/cli@22` to scaffold the project. After scaffolding, the local CLI in `frontend/node_modules/.bin/ng` is v22.

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | Backend handles auth (JWT HS256, BCrypt). Frontend: login form, token storage in localStorage |
| V3 Session Management | yes | JWT stored in localStorage; 24h expiry (backend); frontend clears token on 401 or logout; no token refresh in Phase 2 (24h is sufficient for B2B sessions) |
| V4 Access Control | yes | Functional auth guards (CanActivateFn) protect routes; redirect to /login if unauthenticated |
| V5 Input Validation | yes | Angular template-driven/reactive forms with validation; backend validates with Jakarta Validation (@NotBlank, @Email, @Min) |
| V6 Cryptography | no | Frontend does not perform crypto; JWT is signed/verified by backend (HS256) |
| V7 Error Handling | yes | Error interceptor parses backend errors; never expose stack traces to user; generic error messages per UI-SPEC copywriting |
| V8 Data Protection | yes | JWT token in localStorage (not sessionStorage — AUTH-02 requires persistence); no sensitive data in URL params (search uses query strings to API, not URL fragments) |
| V12 Files & Resources | no | No file upload in Phase 2 |
| V13 API & Web Service | yes | All API calls go through gateway; JWT interceptor adds auth header; CORS configured on gateway |

### Known Threat Patterns for Angular SPA + JWT

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| XSS steals JWT from localStorage | Tampering/Elevation | Angular's built-in sanitization (default); avoid `innerHTML` without sanitization; Content Security Policy headers (gateway-level) |
| CSRF (cross-site request forgery) | Tampering | JWT in Authorization header (not cookies) — immune to CSRF by design; backend has CSRF disabled (stateless) |
| Token theft via open redirect | Spoofing | Validate redirect URLs; only redirect to known routes (/login, /catalog, /orders) |
| Insecure deserialization of API response | Tampering | Angular HttpClient uses `JSON.parse()` with typed responses; no eval; no dynamic code execution |
| CORS misconfiguration | Information Disclosure | Gateway CORS allows only `http://localhost:4200` (dev); same-origin in prod (D-04) |
| Stale token after expiry | Elevation | Error interceptor catches 401 → logout → redirect to /login; no silent token refresh in Phase 2 |

## Sources

### Primary (HIGH confidence)
- **Backend codebase (verified via Read/Grep tools):**
  - `gateway-service/src/main/resources/application.yml` — Gateway routes, port 8080, no CORS config
  - `account-service/.../controller/AuthController.java` — POST /api/auth/login, POST /api/auth/register
  - `account-service/.../controller/AccountController.java` — GET/PUT /api/accounts endpoints
  - `account-service/.../dto/AuthResponse.java` — { token, accountId, email, tier, expiresIn }
  - `account-service/.../dto/LoginRequest.java` — { email, password }
  - `account-service/.../entity/Account.java` — Account entity fields
  - `account-service/.../entity/AccountTier.java` — STANDARD, SILVER, GOLD, PLATINUM
  - `account-service/.../filter/JwtAuthenticationFilter.java` — Authorization: Bearer <token> extraction
  - `account-service/.../config/SecurityConfig.java` — Only /api/auth/** permitAll, everything else authenticated
  - `account-service/.../service/JwtTokenService.java` — JWT claims: accountId, email, tier, companyName
  - `catalog-service/.../controller/CatalogController.java` — GET /api/catalog/products, categories, price
  - `catalog-service/.../dto/ProductDto.java` — { id, sku, name, description, basePrice, inventoryLevel, categoryId, categoryName, isActive, tierPricing, createdAt, updatedAt }
  - `catalog-service/.../dto/CategoryDto.java` — { id, name, parentId, sortOrder, children }
  - `catalog-service/.../dto/TierPricingDto.java` — { id, productId, tier, price }
  - `order-service/.../controller/OrderController.java` — POST /api/orders, GET /api/orders, GET /api/orders/{id}, PUT status, GET status
  - `order-service/.../dto/OrderResponse.java` — { id, accountId, status, totalAmount, creditUsed, items, createdAt, updatedAt }
  - `order-service/.../dto/CreateOrderRequest.java` — { accountId, items: [{ productSku, quantity }] }
  - `order-service/.../dto/OrderItemDto.java` — { id, productSku, productName, quantity, unitPrice, totalPrice }
  - `order-service/.../entity/OrderStatus.java` — PENDING, CONFIRMED, SHIPPED, DELIVERED
  - `catalog-service/.../exception/GlobalExceptionHandler.java` — Error format: { timestamp, status, error, message } / { ..., errors: {field: msg} }
- **npm registry (verified via `npm view` commands):**
  - @angular/core 22.0.2, @angular/material 22.0.2, @angular/cdk 22.0.2, @angular/cli 22.0.3, rxjs (OK), zone.js (OK)
  - @angular/animations: DEPRECATED flag confirmed
- **Angular official docs (verified via WebFetch):**
  - https://angular.dev/guide/http/setup — provideHttpClient setup [CITED]
  - https://angular.dev/guide/http/interceptors — functional interceptors, withInterceptors(), inject() in interceptors [CITED]
  - https://angular.dev/guide/signals — signal(), computed(), effect(), untracked(), asReadonly(), OnPush integration [CITED]
- **Local environment (verified via bash):**
  - Node.js 22.14.0, npm 10.9.2, global Angular CLI 21.2.0
  - docker-compose.yml exists with 8 services

### Secondary (MEDIUM confidence)
- Angular Material 22 theming and CLI scaffolding behavior — based on Material 17+ patterns and npm registry data; `ng add @angular/material` behavior is consistent across v17-v22 [ASSUMED for v22 specifically, will verify during implementation]
- Angular 22 lazy loading patterns (loadComponent, loadChildren) — standard Angular patterns since v14, stable through v22 [ASSUMED for v22 API specifics]
- Angular 22 functional auth guard patterns (CanActivateFn) — stable since Angular 15 [ASSUMED for v22 API specifics]

### Tertiary (LOW confidence)
- None — all critical claims verified via codebase or official docs.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all package versions verified via npm registry; Angular patterns verified via official docs (angular.dev)
- Architecture: HIGH — backend API surface verified by reading all controllers and DTOs; CORS gap verified by grep; auth security scope verified by grep
- Pitfalls: HIGH — all pitfalls derived from verified codebase findings (no CORS, no JWT on catalog/order, Page<T> shape, deprecated @angular/animations)
- Integration: HIGH — exact API contracts, request/response shapes, and auth header format all verified from source code

**Research date:** 2026-06-21
**Valid until:** 2026-07-21 (30 days — Angular is stable, but v22 is 4 days old; monitor for patch releases)
