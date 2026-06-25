# B2B Auto Parts E-Commerce Platform — Application Architecture

## 1. System Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CLIENT LAYER                                       │
│                                                                             │
│   ┌──────────────────┐          ┌──────────────────────────┐               │
│   │  Angular 19 SPA  │──────────│   Python Chatbot UI      │               │
│   │  localhost:4200   │          │   (embedded in Angular)  │               │
│   │  Material MD3     │          │                          │               │
│   └────────┬─────────┘          └──────────┬───────────────┘               │
│            │  /api/*                        │  /api/chat/*                  │
└────────────┼───────────────────────────────┼───────────────────────────────┘
             │                               │
┌────────────┼───────────────────────────────┼───────────────────────────────┐
│            ▼      API GATEWAY LAYER        ▼                               │
│   ┌─────────────────────────────────────────────────┐                      │
│   │        Spring Cloud Gateway (port 8080)         │                      │
│   │  ┌─────────────┬──────────┬──────────┬────────┐ │                      │
│   │  │ /api/auth/* │/api/cat/*│/api/ord/*│/api/ch*│ │                      │
│   │  │ /api/acct/* │          │          │        │ │                      │
│   │  └──────┬──────┴────┬─────┴────┬─────┴───┬────┘ │                      │
│   │         │  JWT Auth  │ CORS     │ Static  │      │                      │
│   │         │  Filter    │          │ Files   │      │                      │
│   └─────────┼───────────┼──────────┼─────────┼──────┘                      │
└─────────────┼───────────┼──────────┼─────────┼─────────────────────────────┘
              │           │          │         │
┌─────────────┼───────────┼──────────┼─────────┼─────────────────────────────┐
│             ▼           ▼          ▼         ▼   MICROSERVICES LAYER       │
│                                                                             │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────────┐   │
│  │   Account    │ │   Catalog    │ │    Order     │ │  Chatbot Agents  │   │
│  │   Service    │ │   Service    │ │    Service   │ │  (Python/FastAPI)│   │
│  │   :8081      │ │   :8082      │ │    :8083     │ │  :8090           │   │
│  │              │ │  + MCP Server│ │  + MCP Server│ │  LangGraph       │   │
│  │  JWT Auth    │ │  Inventory   │ │  Order CRUD  │ │  Multi-Agent     │   │
│  │  Accounts    │ │  Tier Pricing│ │  Status Mgmt │ │  Supervisor      │   │
│  │  API Keys    │ │  Categories  │ │  Events      │ │                  │   │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └────────┬─────────┘   │
│         │                │                │                   │             │
│         │   REST (sync)  │◀───────────────│    MCP (SSE)      │             │
│         │                │  OrderCreated  │◀──────────────────┘             │
│         │                │  OrderCancelled│                                 │
└─────────┼────────────────┼────────────────┼─────────────────────────────────┘
          │                │                │
┌─────────┼────────────────┼────────────────┼─────────────────────────────────┐
│         ▼                ▼                ▼       DATA LAYER                │
│                                                                             │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐       │
│  │  PostgreSQL  │ │  PostgreSQL  │ │  PostgreSQL  │ │   RabbitMQ   │       │
│  │  account_db  │ │  catalog_db  │ │  order_db    │ │   :5672      │       │
│  │  :5432       │ │  :5433       │ │  :5434       │ │   :15672 mgmt│       │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘       │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Frontend | Angular + Angular Material | 19.2.x / MD3 |
| API Gateway | Spring Cloud Gateway | 2024.0.3 |
| Backend | Spring Boot | 3.5.15 (Java 17) |
| AI/Chatbot | Python + FastAPI + LangGraph | 3.11 / 0.115 / 1.2.6 |
| LLM | Groq (Llama 3.3 70B Versatile) | cloud API |
| MCP | Spring AI MCP Server + langchain-mcp-adapters | 1.0.0-M6 / 0.3.0 |
| Messaging | RabbitMQ + Spring Cloud Stream | 3.x / 4.2.1 |
| Database | PostgreSQL (per-service) | 15-alpine |
| ORM | Hibernate/JPA | Spring Boot managed |
| Auth | JWT (HMAC-SHA256) + API Keys | jjwt 0.12.6 |
| Build | Maven multi-module + Angular CLI + pip | 3.9 / 19.x |
| Container | Docker Compose | multi-stage builds |

---

## 3. Service Architecture

### 3.1 Account Service (`:8081`)

**Responsibility:** Identity, authentication, B2B account management, tier-based pricing rules

```
com.btob.account
├── config/
│   └── SecurityConfig.java          — Spring Security, BCrypt, JWT filter chain
├── controller/
│   ├── AuthController.java          — POST /api/auth/login, /api/auth/register
│   └── AccountController.java       — GET/PUT /api/accounts/{id}, credit-limit, balance
├── dto/
│   ├── AccountDto.java              — Registration request
│   ├── AuthResponse.java            — JWT token + accountId + tier
│   └── LoginRequest.java            — email + password
├── entity/
│   ├── Account.java                 — id, email, passwordHash, companyName, tier, creditLimit, currentBalance, apiKey
│   └── AccountTier.java             — STANDARD, SILVER, GOLD, PLATINUM
├── exception/
│   └── GlobalExceptionHandler.java  — Unified error responses
├── filter/
│   └── JwtAuthenticationFilter.java — Extracts Bearer token, validates, sets SecurityContext
├── repository/
│   └── AccountRepository.java       — findByEmail, findByApiKey, existsByEmail
└── service/
    ├── AccountService.java          — Login, register, CRUD, credit management
    ├── ApiKeyService.java           — API key generation and validation
    ├── CustomUserDetailsService.java — Spring Security UserDetailsService
    └── JwtTokenService.java         — JWT create/validate/extract claims
```

**Data Model:**
```
accounts
├── id              UUID PK
├── email           VARCHAR UNIQUE NOT NULL
├── password_hash   VARCHAR NOT NULL (BCrypt)
├── company_name    VARCHAR
├── tier            ENUM (STANDARD|SILVER|GOLD|PLATINUM)
├── credit_limit    DECIMAL(12,2)
├── current_balance DECIMAL(12,2) DEFAULT 0
├── api_key         VARCHAR UNIQUE
├── created_at      TIMESTAMP
└── updated_at      TIMESTAMP
```

**Security Model:**
- Password: BCrypt hashed
- JWT: HMAC-SHA256, 24h expiry, claims = {accountId, email, tier, companyName}
- API Keys: UUID-based, per-account, for service-to-service calls
- Public endpoints: `/api/auth/**`, `/swagger-ui/**`, `/actuator/**`

---

### 3.2 Catalog Service (`:8082`)

**Responsibility:** Product catalog, categories, tier-based pricing, inventory management, MCP server

```
com.btob.catalog
├── config/
│   └── McpServerConfig.java         — Registers MCP tools as ToolCallbackProvider
├── controller/
│   └── CatalogController.java       — Products (search/filter/crud), categories, stock, inventory adjust
├── dto/
│   ├── ProductDto.java              — Full product with tier pricing
│   ├── CategoryDto.java             — Hierarchical category
│   └── TierPricingDto.java          — Per-tier price
├── entity/
│   ├── Product.java                 — sku, name, basePrice, inventoryLevel, reservedInventory, reorderPoint, @Version
│   ├── Category.java                — Hierarchical (self-referencing parent)
│   ├── TierPricing.java             — product + tier + price (unique constraint)
│   └── AccountTier.java             — Duplicated enum (no cross-service dep)
├── event/
│   ├── OrderCreatedEvent.java       — Consumed from RabbitMQ
│   ├── OrderCancelledEvent.java     — Consumed from RabbitMQ
│   └── InventoryAdjustmentEvent.java — Published on admin adjust
├── mcp/
│   └── CatalogMcpTools.java         — search_products, get_product_by_sku, get_low_stock_items, check_stock
├── repository/
│   ├── ProductRepository.java       — searchProducts(JPQL), findBySkuForUpdate (pessimistic lock), findLowStockProducts
│   ├── CategoryRepository.java      — Root categories, children
│   └── TierPricingRepository.java   — findByProductSkuAndTier
└── service/
    ├── CatalogService.java          — Core logic: reserve/commit/release inventory with @Retryable
    ├── DataInitializer.java         — Seeds 5 categories, 20 products, tier pricing
    └── OrderEventConsumer.java      — Idempotent event handler (ConcurrentHashMap tracking)
```

**Data Model:**
```
categories
├── id          UUID PK
├── name        VARCHAR NOT NULL
├── parent_id   UUID FK → categories (self-ref)
└── sort_order  INT DEFAULT 0

products
├── id                 UUID PK
├── sku                VARCHAR(100) UNIQUE NOT NULL
├── name               VARCHAR NOT NULL
├── description        TEXT
├── base_price         DECIMAL(10,2) NOT NULL
├── inventory_level    INT DEFAULT 0
├── reserved_inventory INT DEFAULT 0
├── reorder_point      INT DEFAULT 10
├── version            BIGINT (optimistic locking)
├── category_id        UUID FK → categories
├── is_active          BOOLEAN DEFAULT TRUE
├── created_at         TIMESTAMP
└── updated_at         TIMESTAMP

tier_pricing
├── id         UUID PK
├── product_id UUID FK → products
├── tier       ENUM (STANDARD|SILVER|GOLD|PLATINUM)
├── price      DECIMAL(10,2) NOT NULL
└── UNIQUE(product_id, tier)
```

**Inventory Concurrency Model:**
```
Reserve (on order create):
  1. SELECT ... FOR UPDATE (pessimistic lock) OR @Retryable with Optimistic Locking
  2. reserved_inventory += quantity
  3. COMMIT

Commit (on order confirm):
  1. inventory_level -= quantity
  2. reserved_inventory -= quantity
  3. COMMIT

Release (on order cancel):
  1. reserved_inventory -= quantity
  2. If wasConfirmed: inventory_level += quantity (restore committed stock)
  3. COMMIT
```

**API Endpoints:**
| Method | Path | Description |
|---|---|---|
| GET | `/api/catalog/products` | Search/filter/paginate products |
| GET | `/api/catalog/products/{sku}` | Get product by SKU |
| GET | `/api/catalog/products/{sku}/price` | Tier-specific price |
| GET | `/api/catalog/categories` | Category tree |
| POST | `/api/catalog/products` | Create product |
| GET | `/api/catalog/products/low-stock` | Below reorder point |
| GET | `/api/catalog/products/{sku}/stock` | Available count |
| PATCH | `/api/catalog/products/{sku}/inventory` | Admin delta adjust |

---

### 3.3 Order Service (`:8083`)

**Responsibility:** Order lifecycle management, status tracking, MCP server

```
com.btob.order
├── config/
│   ├── AppConfig.java               — RestTemplate bean for catalog calls
│   └── McpServerConfig.java         — Registers MCP tools
├── controller/
│   └── OrderController.java         — CRUD + status transitions + cancel
├── dto/
│   ├── CreateOrderRequest.java      — accountId + items[]
│   ├── OrderItemDto.java            — SKU, name, qty, pricing
│   └── OrderResponse.java           — Full order with items
├── entity/
│   ├── Order.java                   — accountId, status, totalAmount, creditUsed, items
│   ├── OrderItem.java               — productSku, productName, quantity, unitPrice, totalPrice
│   └── OrderStatus.java             — PENDING|CONFIRMED|SHIPPED|DELIVERED|CANCELLED
├── event/
│   ├── OrderCreatedEvent.java       — Published to RabbitMQ
│   ├── OrderStatusChangedEvent.java — Published to RabbitMQ
│   └── OrderCancelledEvent.java     — Published to RabbitMQ
├── mcp/
│   └── OrderMcpTools.java           — check_order_status, list_orders, create_b2b_order
├── repository/
│   ├── OrderRepository.java         — findByAccountId, findByAccountIdAndStatus
│   └── OrderItemRepository.java     — findByOrderId
└── service/
    ├── OrderService.java            — Create (fetches product via REST), status transitions, cancel
    └── OrderEventPublisher.java     — StreamBridge for order events
```

**Data Model:**
```
orders
├── id            UUID PK
├── account_id    UUID NOT NULL
├── status        ENUM (PENDING|CONFIRMED|SHIPPED|DELIVERED|CANCELLED)
├── total_amount  DECIMAL(12,2)
├── credit_used   DECIMAL(12,2)
├── created_at    TIMESTAMP
└── updated_at    TIMESTAMP

order_items
├── id             UUID PK
├── order_id       UUID FK → orders
├── product_sku    VARCHAR(100) NOT NULL
├── product_name   VARCHAR (denormalized)
├── quantity       INT NOT NULL
├── unit_price     DECIMAL(10,2) NOT NULL
└── total_price    DECIMAL(10,2) NOT NULL
```

**Status State Machine:**
```
PENDING ──────► CONFIRMED ──────► SHIPPED ──────► DELIVERED
   │                │
   └──► CANCELLED ◄─┘

Terminal states: DELIVERED, CANCELLED
```

**Order Creation Flow:**
```
1. Receive CreateOrderRequest {accountId, items[{sku, quantity}]}
2. For each item: GET http://catalog-service:8082/api/catalog/products/{sku}
   (Fallback: hardcoded product map if catalog down)
3. Denormalize productName, unitPrice into OrderItem
4. Calculate totalAmount = sum(unitPrice * quantity)
5. Save Order + OrderItems (cascade)
6. Publish OrderCreatedEvent → RabbitMQ "order-events"
7. Catalog consumes → reserves inventory per item
```

---

### 3.4 Gateway Service (`:8080`)

**Responsibility:** Single entry point, routing, CORS, JWT validation, static file serving

```
com.btob.gateway
├── GatewayApplication.java
└── config/
    ├── CorsConfig.java              — CorsWebFilter for localhost:4200
    └── StaticResourceConfig.java    — Serves Angular SPA + hashed asset caching
```

**Route Table:**
```
┌─────────────────────┬──────────────────────────┬────────────────┐
│ Predicate           │ Target URI               │ Filter         │
├─────────────────────┼──────────────────────────┼────────────────┤
│ /api/auth/**        │ http://account-service:8081 │ StripPrefix=0 │
│ /api/accounts/**    │ http://account-service:8081 │ StripPrefix=0 │
│ /api/catalog/**     │ http://catalog-service:8082  │ StripPrefix=0 │
│ /api/orders/**      │ http://order-service:8083    │ StripPrefix=0 │
│ /api/chat/**        │ http://chatbot-agents:8090   │ StripPrefix=1 │
│ /api/chatbot/health │ http://chatbot-agents:8090   │ StripPrefix=1 │
│ /** (non-/api/)     │ classpath:/static/        │ SPA fallback   │
└─────────────────────┴──────────────────────────┴────────────────┘
```

**Static File Strategy:**
- Angular build output → `classpath:/static/`
- Hashed assets (`*.abc123def.js`) → Cache-Control: 30 days
- All non-API, non-file paths → serve `index.html` (SPA client-side routing)

---

### 3.5 Chatbot Agents (`:8090`)

**Responsibility:** AI-powered natural language interface via multi-agent system

```
chatbot-agents/
├── main.py              — FastAPI app, session management, agent orchestration
├── auth.py              — JWT validation (same HS256 secret as Java)
├── supervisor.py        — LangGraph StateGraph: intent classification → routing
├── orders_agent.py      — ReAct agent with order MCP tools
├── catalog_agent.py     — ReAct agent with catalog MCP tools
├── mcp_client.py        — MultiServerMCPClient wrapper (SSE transport)
└── tests/               — Pytest suite for auth, supervisor, MCP client
```

**Agent Architecture:**
```
                         ┌─────────────────────┐
                         │   User Message       │
                         └──────────┬──────────┘
                                    │
                         ┌──────────▼──────────┐
                         │   Supervisor Agent   │
                         │  (Llama 3.3 70B)     │
                         │  Intent Classification│
                         └──────────┬──────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
            ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
            │ Orders Agent │ │Catalog Agent │ │Direct Agent  │
            │ (ReAct)      │ │ (ReAct)      │ │(Simple LLM)  │
            │              │ │              │ │              │
            │ MCP Tools:   │ │ MCP Tools:   │ │ No tools     │
            │ list_orders  │ │search_products│ │ Greetings    │
            │check_status  │ │get_by_sku    │ │ Simple Q&A   │
            │create_order  │ │check_stock   │ │              │
            └──────┬───────┘ └──────┬───────┘ └──────────────┘
                   │                │
                   ▼                ▼
            ┌─────────────────────────────────┐
            │  MCP SSE Transport               │
            │  → order-service:8083/sse        │
            │  → catalog-service:8082/sse      │
            └─────────────────────────────────┘
```

**Request Flow:**
```
1. POST /api/chat/send {messages: [{role: "user", content: "show my orders"}]}
2. Gateway strips /api → forwards to chatbot-agents:8090/chat/send
3. FastAPI validates JWT → extracts accountId from claims
4. Session store: sessions[accountId] maintains conversation history
5. Injects system msg: "The current user's accountId is: <UUID>"
6. supervisor_graph.ainvoke({messages: session_messages})
7. Router LLM → classifies intent → routes to orders/catalog/direct
8. Agent ReAct loop → invokes MCP tool → gets result → generates response
9. Response returned through gateway to frontend
```

---

## 4. Communication Patterns

### 4.1 Synchronous (REST)

| From | To | Endpoint | Purpose |
|---|---|---|---|
| Order Service | Catalog Service | `GET /api/catalog/products/{sku}` | Fetch product details during order creation |
| Frontend | Gateway | `GET/POST/PUT/PATCH /api/**` | All UI operations |
| Chatbot | Gateway | `POST /api/chat/send` | Chat messages |

### 4.2 Asynchronous (RabbitMQ / Spring Cloud Stream)

```
┌────────────────┐                    ┌──────────┐                    ┌────────────────┐
│  order-service │──OrderCreated─────▶│ RabbitMQ │──OrderCreated────▶│ catalog-service │
│                │──OrderCancelled───▶│          │──OrderCancelled──▶│                │
│                │                    │          │                    │ (reserve/release│
│                │◀──────────────────│          │                    │  inventory)     │
└────────────────┘                    └──────────┘                    └────────────────┘
                                            ▲
┌────────────────┐                         │
│ account-service│──AccountUpdated─────────┘
└────────────────┘
```

| Exchange | Events | Publisher | Consumer |
|---|---|---|---|
| `order-events` | OrderCreated, OrderStatusChanged | order-service | catalog-service |
| `order-cancelled-events` | OrderCancelled | order-service | catalog-service |
| `catalog-events` | InventoryAdjustment | catalog-service | (audit log) |
| `account-events` | AccountUpdated | account-service | (audit log) |

**Idempotency:** `OrderEventConsumer` tracks processed orderIds in `ConcurrentHashMap.newKeySet()`. Duplicate events are silently dropped.

### 4.3 MCP (Model Context Protocol)

| MCP Server | Transport | Endpoint | Tools |
|---|---|---|---|
| order-service | SSE | `http://order-service:8083/sse` | `check_order_status`, `list_orders`, `create_b2b_order` |
| catalog-service | SSE | `http://catalog-service:8082/sse` | `search_products`, `get_product_by_sku`, `get_low_stock_items`, `check_stock` |

Tools are annotated with `@Tool` / `@ToolParam` (Spring AI) and exposed as `ToolCallbackProvider` beans. Python agents consume them via `langchain-mcp-adapters`.

---

## 5. Security Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Auth Flow                          │
│                                                      │
│  1. Client → POST /api/auth/login {email, password}  │
│  2. AccountService → BCrypt verify                   │
│  3. JwtTokenService → Generate JWT (HS256, 24h)      │
│     Claims: {accountId, email, tier, companyName}    │
│  4. Return AuthResponse {token, accountId, tier}     │
│  5. Client stores JWT in localStorage                │
│  6. authInterceptor adds Authorization: Bearer <jwt> │
│  7. Gateway forwards to downstream services          │
│  8. JwtAuthenticationFilter validates + sets context │
└─────────────────────────────────────────────────────┘
```

**Shared JWT Secret:** Base64-encoded `secret-key-for-development-only-not-in-prod` — identical across account-service, gateway, chatbot-agents.

**Per-Service Security:**
| Service | Spring Security | JWT Filter | Public Endpoints |
|---|---|---|---|
| account-service | Yes | Yes | `/api/auth/**`, `/swagger-ui/**`, `/actuator/**` |
| catalog-service | No | No | All public (protected by gateway) |
| order-service | No | No | All public (protected by gateway) |
| gateway-service | No | No | Routes enforce authentication |
| chatbot-agents | No | Custom Python JWT | `/health`, `/agents`; `/chat` requires JWT |

**API Key Authentication:** Per-account UUID-based keys stored in `accounts.api_key` column. Used for service-to-service calls (e.g., `ApiKeyService.validateApiKey()`).

---

## 6. Frontend Architecture

### 6.1 Component Tree
```
AppComponent (Material sidenav + toolbar + cart sidebar)
├── LoginComponent          — /login
├── RegisterComponent       — /register
├── CatalogComponent        — /catalog (product grid, search, filter, sort, pagination)
│   └── ProductCardComponent — Product card with tier pricing + add-to-cart
├── OrdersComponent         — /orders (order history table)
│   └── OrderDetailComponent — /orders/:id (detail + cancel)
├── CartSidebarComponent    — Slide-out cart (stock check, place order)
│   └── CartConfirmationDialogComponent — Confirmation modal
├── ChatComponent           — /chat (AI chatbot interface)
├── InventoryListComponent  — /inventory (admin inventory management)
├── TierBadgeComponent      — Shared: colored tier badge
└── StatusChipComponent     — Shared: colored status chip
```

### 6.2 State Management (Angular Signals)
```
AuthService:      signal(currentUser), signal(token), computed(isAuthenticated)
CartService:      signal(items[]), computed(total), computed(itemCount)
CatalogComponent: signal(products[]), signal(categories[]), signal(loading)
OrdersComponent:  signal(orders[]), signal(loading)
ChatComponent:    signal(messages[]), signal(isTyping)
```

### 6.3 Route Configuration
```
/login        → LoginComponent (public)
/register     → RegisterComponent (public)
/catalog      → CatalogComponent (authGuard)
/catalog/:sku → ProductCardComponent detail view
/orders       → OrdersComponent (authGuard)
/orders/:id   → OrderDetailComponent (authGuard)
/chat         → ChatComponent (authGuard)
/inventory    → InventoryListComponent (authGuard)
```

All feature routes use `loadChildren`/`loadComponent` for lazy loading.

### 6.4 Interceptors
- **authInterceptor:** Attaches `Authorization: Bearer <token>` to `/api/` requests (except `/api/auth/`)
- **errorInterceptor:** Catches 401 → auto-logout + redirect to `/login`; extracts error.message from backend

---

## 7. Data Flow: Complete Order Lifecycle

```
1. BROWSE CATALOG
   User → Angular → Gateway → CatalogService → products with tier pricing

2. ADD TO CART
   Angular CartService (local state) → stock check via CatalogService

3. PLACE ORDER
   User → Angular → Gateway → OrderService.createOrder()
   ├── Fetch product details from CatalogService (REST)
   ├── Save Order + OrderItems to order_db
   ├── Publish OrderCreatedEvent → RabbitMQ
   └── CatalogService consumes → reserveInventory() per item

4. CONFIRM ORDER
   Admin → Gateway → OrderService.updateOrderStatus(CONFIRMED)
   ├── Validate: PENDING → CONFIRMED
   └── Publish OrderStatusChangedEvent → RabbitMQ

5. SHIP ORDER
   Admin → Gateway → OrderService.updateOrderStatus(SHIPPED)
   ├── Validate: CONFIRMED → SHIPPED
   └── CatalogService: commitReservation() (deducts actual stock)

6. CANCEL ORDER
   User → Gateway → OrderService.cancelOrder()
   ├── Validate: PENDING or CONFIRMED
   ├── Publish OrderCancelledEvent → RabbitMQ
   └── CatalogService: releaseReservation() (restores available + reserved)

7. AI CHAT
   User → Angular ChatComponent → Gateway → ChatbotAgents
   ├── JWT validation → accountId extraction
   ├── Supervisor routes to orders/catalog agent
   └── Agent invokes MCP tool → Java service → response
```

---

## 8. Deployment Architecture

```
docker-compose.yml
├── Infrastructure
│   ├── account-db      (postgres:15-alpine, port 5432)
│   ├── catalog-db      (postgres:15-alpine, port 5433)
│   ├── order-db        (postgres:15-alpine, port 5434)
│   └── rabbitmq        (rabbitmq:3-management-alpine, ports 5672/15672)
│
├── Application Services (multi-stage Docker builds)
│   ├── account-service  (Dockerfile.account,  port 8081)
│   ├── catalog-service  (Dockerfile.catalog,  port 8082)
│   ├── order-service    (Dockerfile.order,    port 8083)
│   ├── gateway-service  (Dockerfile.gateway,  port 8080)
│   └── chatbot-agents   (Dockerfile.python,   port 8090)
│
├── Health Checks
│   ├── Java services: wget /actuator/health (30s interval, 60s start period)
│   └── Python: python urllib /health (30s interval, 30s start period)
│
├── Dependencies
│   ├── gateway → account, catalog, order services
│   ├── chatbot → order-service, catalog-service
│   └── All Java services → their respective DB + RabbitMQ
│
└── Volumes
    ├── account-db-data, catalog-db-data, order-db-data
    └── rabbitmq-data
```

**Docker Build Pattern (all Java services):**
```
Stage 1 (build): maven:3.9-eclipse-temurin-17
  → COPY entire project → mvn clean package -DskipTests -pl {service} -am -B

Stage 2 (runtime): eclipse-temurin:17-jre-alpine
  → COPY jar as app.jar → java -jar app.jar
```

---

## 9. Key Design Decisions

| Decision | Rationale |
|---|---|
| **Database per service** | Full isolation, independent schema evolution, no cross-service DB joins |
| **Event-driven inventory** | Decouples order creation from inventory reservation; eventual consistency acceptable |
| **Optimistic + Pessimistic locking** | Catalog uses both: @Retryable for normal load, `SELECT FOR UPDATE` for high contention |
| **MCP protocol for AI** | Standardized tool interface; Java services expose tools, Python agents consume them |
| **LangGraph supervisor pattern** | LLM-based intent routing avoids hardcoded keyword matching |
| **Gateway as SPA server** | Single origin for production; eliminates CORS complexity |
| **Per-service seed data** | `ON CONFLICT DO NOTHING` + `defer-datasource-initialization` for idempotent startup |
| **Shared JWT secret** | Development simplicity; production would use asymmetric keys or a central auth server |
| **RestTemplate fallback** | Order service falls back to hardcoded product data if catalog is down |

---

## 10. API Reference Summary

### Account Service
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/login` | Login, returns JWT |
| POST | `/api/auth/register` | Register new account |
| GET | `/api/accounts/{id}` | Get account |
| GET | `/api/accounts` | List all accounts |
| PUT | `/api/accounts/{id}/credit-limit` | Update credit limit |
| PUT | `/api/accounts/{id}/balance` | Update balance |

### Catalog Service
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/catalog/products` | Search/filter/paginate |
| GET | `/api/catalog/products/{sku}` | Get by SKU |
| GET | `/api/catalog/products/{sku}/price` | Tier price |
| GET | `/api/catalog/categories` | Category tree |
| POST | `/api/catalog/products` | Create product |
| GET | `/api/catalog/products/low-stock` | Low stock alert |
| GET | `/api/catalog/products/{sku}/stock` | Available count |
| PATCH | `/api/catalog/products/{sku}/inventory` | Adjust inventory |

### Order Service
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/orders` | Create order |
| GET | `/api/orders/{id}` | Get order |
| GET | `/api/orders` | List orders (paginated) |
| PUT | `/api/orders/{id}/status` | Update status |
| POST | `/api/orders/{id}/cancel` | Cancel order |
| GET | `/api/orders/{id}/status` | Get status only |

### Chatbot
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/chat/send` | Send message (JWT required) |
| GET | `/health` | Health check |
| GET | `/agents` | List agents and tools |

---

## 11. Seed Data

| Entity | Count | Details |
|---|---|---|
| Accounts | 3 | ACME/STANDARD/$10K, Globex/SILVER/$25K, Initech/GOLD/$50K |
| Categories | 5 | Brakes, Engine, Electrical, Suspension, Filters |
| Products | 20 | Auto parts with tier pricing (SILVER -10%, GOLD -15%, PLATINUM -20%) |
| Orders | 5 | Across all accounts, various statuses |
| Order Items | 15 | 1-5 items per order |

---

## 12. Current Known Issues

1. **Catalog agent tool calling:** Groq/llama-3.3-70b sometimes generates function calls as text (`<function=search_products ...>`) instead of structured JSON. Affects catalog search through chatbot.

2. **JWT secret in source:** Shared HS256 secret is in application.yml and .env. Production should use asymmetric keys or a centralized auth service.

3. **Hardcoded catalog URL:** Order service calls catalog at `http://localhost:8082` directly. Should use service discovery or environment variable.

4. **In-memory sessions:** Chatbot session storage is per-container. Not persistent across restarts or horizontally scalable.
