# Requirements: B2B Auto Parts E-commerce Platform

**Defined:** 2026-06-21
**Core Value:** B2B customers can autonomously check order status and place new orders through an AI chatbot

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Authentication

- [x] **AUTH-01**: B2B user can log in with email/password and receive JWT token
- [x] **AUTH-02**: JWT token persists across browser refresh for session management
- [x] **AUTH-03**: B2B accounts have API keys for service-to-service authentication

### Catalog

- [ ] **CATALOG-01**: Auto parts catalog displays items with SKU, name, description, price, and inventory level
- [ ] **CATALOG-02**: Users can search and filter parts by category, name, or SKU
- [ ] **CATALOG-03**: B2B pricing tiers display different prices based on account level

### Orders

- [ ] **ORDER-01**: Users can create new B2B orders with line items (SKU + quantity)
- [ ] **ORDER-02**: Order status tracks PENDING, SHIPPED, DELIVERED states
- [ ] **ORDER-03**: Users can view order history and check order status

### Accounts

- [x] **ACCT-01**: B2B accounts have credit limits and current balance tracking
- [x] **ACCT-02**: Account tier determines pricing level and credit terms

### Chatbot

- [ ] **CHAT-01**: MCP tool check_order_status(orderId) returns order status
- [ ] **CHAT-02**: MCP tool create_b2b_order(accountId, items) creates new order

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Advanced Features

- **ADV-01**: Real-time inventory updates via WebSocket
- **ADV-02**: Bulk order import via CSV upload
- **ADV-03**: Order approval workflow for large orders
- **ADV-04**: Multi-location inventory tracking

### Integrations

- **INT-01**: ERP system integration for inventory sync
- **INT-02**: Shipping carrier API integration
- **INT-03**: Payment gateway integration

## Out of Scope

| Feature | Reason |
|---------|--------|
| Payment processing | Use mock/simulation for MVP |
| Multi-warehouse inventory | Start with single warehouse |
| Mobile native apps | Web-first approach |
| Complex approval workflows | Start with full autonomy |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | Phase 1 | Complete |
| AUTH-02 | Phase 1 | Complete |
| AUTH-03 | Phase 1 | Complete |
| CATALOG-01 | Phase 1 | Pending |
| CATALOG-02 | Phase 1 | Pending |
| CATALOG-03 | Phase 1 | Pending |
| ORDER-01 | Phase 1 | Pending |
| ORDER-02 | Phase 1 | Pending |
| ORDER-03 | Phase 1 | Pending |
| ACCT-01 | Phase 1 | Complete |
| ACCT-02 | Phase 1 | Complete |
| CHAT-01 | Phase 3 | Pending |
| CHAT-02 | Phase 3 | Pending |

**Coverage:**
- v1 requirements: 13 total
- Mapped to phases: 13
- Unmapped: 0 ✓

---
*Requirements defined: 2026-06-21*
*Last updated: 2026-06-21 after Plan 01-02 completion*