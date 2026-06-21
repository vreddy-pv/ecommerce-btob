# Phase 1: Backend Foundation - Context

**Gathered:** 2026-06-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Create Spring Boot microservices with domain entities, repositories, and REST controllers for B2B accounts, auto parts catalog, and sales orders. This includes account management with credit limits and tier pricing, catalog with SKU/pricing/inventory, and order management with status tracking.

</domain>

<decisions>
## Implementation Decisions

### Microservice Structure
- **D-01:** Deploy as three independent Spring Boot microservices: account-service, catalog-service, and order-service
- **D-02:** Each service has its own database (database-per-service pattern) for loose coupling
- **D-03:** Services communicate via event-driven messaging using Spring Cloud Stream

### Messaging System
- **D-04:** Use Spring Cloud Stream as messaging abstraction layer (supports Kafka and RabbitMQ, can switch later)
- **D-05:** Events for state changes: order creation, order status updates, inventory changes

### API Gateway and Service Discovery
- **D-06:** Use Spring Cloud Gateway as single entry point for all API requests
- **D-07:** Use Eureka for service discovery and registration
- **D-08:** Gateway handles authentication/authorization, routes to appropriate service

### the agent's Discretion
- Database schema design within each service (agent can choose optimal schema)
- REST API response format (agent can use standard Spring patterns)
- Error handling approach (agent can use standard Spring exception handling)
- Pagination strategy (agent can implement appropriate pagination)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project Context
- `.planning/PROJECT.md` — Project goals, constraints, and key decisions
- `.planning/REQUIREMENTS.md` — v1 requirements with acceptance criteria
- `.planning/ROADMAP.md` — Phase definitions and success criteria

### No external specs
No external specs — requirements fully captured in decisions above

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- None — this is a greenfield project

### Established Patterns
- None — patterns will be established in this phase

### Integration Points
- Frontend (Phase 2) will consume REST APIs from gateway
- Chatbot (Phase 3) will use MCP tools that call these services

</code_context>

<specifics>
## Specific Ideas

No specific requirements — open to standard Spring Boot microservices patterns

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 1-Backend Foundation*
*Context gathered: 2026-06-21*