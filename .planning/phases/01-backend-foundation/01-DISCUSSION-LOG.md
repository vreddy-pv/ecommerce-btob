# Phase 1: Backend Foundation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-21
**Phase:** 1-Backend Foundation
**Areas discussed:** Microservice Structure

---

## Microservice Structure

| Option | Description | Selected |
|--------|-------------|----------|
| Modular monolith first | Single Spring Boot app with clear module boundaries (account, catalog, order modules). Split into separate services later when needed. | |
| Separate microservices | Three independent Spring Boot apps (account-service, catalog-service, order-service) with separate databases and inter-service communication | ✓ |
| Hybrid approach | Start with modular monolith, but design interfaces so we can extract services later | |

**User's choice:** Separate microservices
**Notes:** User wants three independent Spring Boot apps with separate databases

---

## Communication

| Option | Description | Selected |
|--------|-------------|----------|
| REST HTTP calls | Simple synchronous REST calls between services. Easy to understand and debug. | |
| Event-driven (Kafka/RabbitMQ) | Asynchronous messaging for order updates, inventory changes. More complex but better scalability. | ✓ |
| Hybrid (REST + Events) | REST for queries, events for state changes like order creation | |

**User's choice:** Event-driven (Kafka/RabbitMQ)
**Notes:** User wants asynchronous messaging for better scalability

---

## Messaging

| Option | Description | Selected |
|--------|-------------|----------|
| Kafka | High throughput, durable, good for event sourcing. More complex setup. | |
| RabbitMQ | Simple AMQP messaging, easy to set up. Good for most use cases. | |
| Spring Cloud Stream | Abstraction layer that supports both Kafka and RabbitMQ. Can switch later. | ✓ |

**User's choice:** Spring Cloud Stream
**Notes:** User wants abstraction layer for flexibility to switch messaging systems later

---

## Gateway

| Option | Description | Selected |
|--------|-------------|----------|
| Spring Cloud Gateway + Eureka | Standard Spring Cloud setup with service discovery and routing | ✓ |
| Simple reverse proxy | Nginx or similar for routing, no service discovery | |
| API Gateway first | Start with gateway as single entry point, add discovery later | |

**User's choice:** Spring Cloud Gateway + Eureka
**Notes:** User wants standard Spring Cloud setup with service discovery

---

## the agent's Discretion

- Database schema design within each service (agent can choose optimal schema)
- REST API response format (agent can use standard Spring patterns)
- Error handling approach (agent can use standard Spring exception handling)
- Pagination strategy (agent can implement appropriate pagination)

## Deferred Ideas

None — discussion stayed within phase scope