# B2B Auto Parts E-commerce Platform

## What This Is

A B2B e-commerce platform for auto parts (similar to O'Reilly Auto Parts B2B portal) that enables business customers (mechanic shops, dealerships) to browse catalogs, place orders, and manage accounts. The platform features an AI-powered chatbot that can autonomously handle customer queries and execute transactions via the Model Context Protocol (MCP).

## Core Value

B2B customers can autonomously check order status and place new orders through an AI chatbot, reducing support overhead and enabling 24/7 self-service.

## Business Context

- **Customer**: B2B auto parts buyers (mechanic shops, dealerships, fleet managers)
- **Revenue model**: Transaction-based e-commerce with tiered B2B pricing
- **Success metric**: Chatbot handles >80% of order inquiries without human intervention

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] B2B account management with credit limits and tier pricing
- [ ] Auto parts catalog with SKU, pricing, and inventory levels
- [ ] Sales order management (create, track, fulfill)
- [ ] Angular frontend with B2B dashboard
- [ ] Spring Boot microservices backend
- [ ] AI chatbot with MCP integration for autonomous order handling
- [ ] Docker-based build and deployment

### Out of Scope

- Payment processing (use mock/simulation for MVP)
- Multi-warehouse inventory management
- Mobile native apps (web-first approach)
- Complex approval workflows (start with full autonomy)

## Context

- Targeting large scale (500+ users) from day one
- Using Groq as the LLM provider for fast inference
- PostgreSQL for production database
- Docker Compose for deployment
- JWT + API Keys for authentication
- Full chatbot autonomy (execute orders without human approval)

## Constraints

- **Tech stack**: Spring Boot 3.x, Java 17+, Angular 17+, PostgreSQL
- **Deployment**: Docker Compose (single-machine initially)
- **AI Layer**: MCP Server in Spring Boot, Groq LLM integration
- **Scale**: Must handle 500+ concurrent B2B users

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Groq for LLM | Fast inference, cost-effective for high-volume chat | — Pending |
| Docker Compose | Easiest to start, can migrate to K8s later | — Pending |
| Full chatbot autonomy | Reduces support overhead, enables 24/7 service | — Pending |
| JWT + API Keys | Industry standard for B2B, supports service-to-service | — Pending |
| PostgreSQL | Production-grade, ACID, great for e-commerce | — Pending |

---
*Last updated: 2026-06-21 after initialization*