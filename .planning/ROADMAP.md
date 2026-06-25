# Roadmap: B2B Auto Parts E-commerce Platform

## Overview

This roadmap builds a B2B auto parts e-commerce platform with Spring Boot microservices backend, Angular frontend, and AI chatbot integration. The approach follows horizontal layers: build complete technical layers (DB → API → UI → wiring) and assemble at the end.

## Phases

- [ ] **Phase 1: Backend Foundation** - Spring Boot microservices with domain entities, repositories, and REST controllers
- [ ] **Phase 2: Angular Frontend** - B2B dashboard with catalog browsing and order management
- [ ] **Phase 3: MCP Server Integration** - AI agent tools for chatbot functionality
- [x] **Phase 4: Chatbot UI & Agent Loop** - Chat interface wired to LLM for intent routing
- [x] **Phase 5: Inventory Management** - Proper inventory with reservations, cancellation, low-stock alerts, and restock

## Phase Details

### Phase 1: Backend Foundation
**Goal**: Create Spring Boot microservices with domain entities, repositories, and REST controllers for B2B accounts, auto parts catalog, and sales orders
**Depends on**: Nothing (first phase)
**Requirements**: [AUTH-01, AUTH-02, AUTH-03, CATALOG-01, CATALOG-02, CATALOG-03, ORDER-01, ORDER-02, ORDER-03, ACCT-01, ACCT-02]
**Success Criteria** (what must be TRUE):
  1. B2B account entity with credit limits and tier pricing exists and can be persisted
  2. Auto part catalog with SKU, pricing, and inventory levels is queryable via REST API
  3. Sales order can be created with line items and status tracking (PENDING, SHIPPED, DELIVERED)
  4. JWT authentication works for B2B users with API key support
**Plans:** 4 plans

Plans:
- [x] 01-01-PLAN.md — Domain model and database schema design
- [x] 01-02-PLAN.md — Account and authentication microservice
- [x] 01-03-PLAN.md — Catalog microservice
- [x] 01-04-PLAN.md — Order microservice

### Phase 2: Angular Frontend
**Goal**: Build B2B dashboard showing parts catalog with search/filter and recent orders table
**Depends on**: Phase 1
**Requirements**: [CATALOG-01, CATALOG-02, CATALOG-03, ORDER-01, ORDER-02, ORDER-03]
**Success Criteria** (what must be TRUE):
  1. Dashboard displays auto parts catalog with search and filtering capabilities
  2. Recent orders table shows order history with status indicators
  3. User can navigate between catalog and order views
**Plans:** 3 plans

Plans:
- [x] 02-01-PLAN.md — Angular project setup, core services, auth shell, routing, gateway CORS
- [x] 02-02-PLAN.md — Catalog browsing components (search, filter, tier pricing)
- [x] 02-03-PLAN.md — Order management components (cart, orders table, order detail)

### Phase 3: MCP Server Integration
**Goal**: Implement MCP Server with check_order_status and create_b2b_order tools for AI agent
**Depends on**: Phase 1
**Requirements**: [CHAT-01, CHAT-02]
**Success Criteria** (what must be TRUE):
  1. MCP tool check_order_status(orderId) returns order status from database
  2. MCP tool create_b2b_order(accountId, items) creates new order and returns order ID
  3. MCP Server is accessible via HTTP/SSE transport
**Plans:** 2 plans

Plans:
- [x] 03-01-PLAN.md — MCP Server setup and configuration
- [x] 03-02-PLAN.md — Tool implementation and testing

### Phase 4: Chatbot UI & Agent Loop
**Goal**: Build chat interface wired to LLM for intent routing to MCP tools
**Depends on**: Phase 2, Phase 3
**Requirements**: (UI requirements for chat interface)
**Success Criteria** (what must be TRUE):
  1. Chat UI accepts user messages and displays responses
  2. LLM routes "check order" intents to check_order_status tool
  3. LLM routes "create order" intents to create_b2b_order tool
  4. Full autonomy: chatbot can execute orders without human approval
**Plans**: TBD

Plans:
- [x] 04-01: Angular chat component
- [x] 04-02: LLM integration and intent routing

### Phase 5: Inventory Management
**Goal**: Add proper inventory management with soft reservations, cancellation/restock, low-stock alerting, and admin restock workflow
**Depends on**: Phase 1, Phase 3
**Requirements**: [INV-01, INV-02, INV-03, INV-04, INV-05]
**Success Criteria** (what must be TRUE):
  1. Placing an order reserves inventory (soft reservation, not hard decrement)
  2. Shipment commits reservation (decrements actual stock)
  3. Cancellation releases reserved inventory (restores available stock)
  4. Low-stock products are queryable via API and visible on dashboard
  5. Admin can adjust inventory with delta-based endpoint (auth-protected)
  6. Concurrency conflicts handled via optimistic locking + retry
**Context**: 05-CONTEXT.md (6 locked decisions)
**Plans**: 4 plans

Plans:
- [x] 05-01 — Catalog-service: entity, service, optimistic locking
- [x] 05-02 — Catalog-service: events, controller, MCP tools
- [x] 05-03 — Order-service: cancel flow, remove mock data
- [x] 05-04 — Frontend: inventory page, low-stock widget, UI fixes

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Backend Foundation | 4/4 | Complete | 2026-06-21 |
| 2. Angular Frontend | 3/3 | Complete | 2026-06-21 |
| 3. MCP Server Integration | 2/2 | Complete | 2026-06-21 |
| 4. Chatbot UI & Agent Loop | 2/2 | Complete | 2026-06-22 |
| 5. Inventory Management | 4/4 | Complete | 2026-06-25 |