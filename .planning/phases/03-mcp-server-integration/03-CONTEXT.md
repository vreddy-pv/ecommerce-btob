# Phase 3: MCP Server Integration — CONTEXT.md

## Architecture Overview

```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────────────┐
│  Angular Chat   │────▶│  Python LangGraph    │────▶│  order-service      │
│  UI (Phase 4)   │     │  Orchestrator        │     │  (MCP Server)       │
│  localhost:4200 │     │  localhost:8090      │     │  localhost:8083     │
─────────────────┘     └──────────────────────┘     └─────────────────────┘
                                                       │
                                                       ▼
                                                ┌─────────────────┐
                                                │  catalog-service │
                                                │  (MCP Server)   │
                                                │  localhost:8082 │
                                                ─────────────────┘
```

## Locked Decisions

### 1. MCP Servers: Embedded in Existing Services
- **order-service**: Exposes `check_order_status` and `create_b2b_order` tools
- **catalog-service**: Exposes `search_products` and `get_product_by_sku` tools
- **Transport**: SSE (Server-Sent Events) via `spring-ai-mcp-server-webmvc-spring-boot-starter`
- **Spring AI Version**: 1.1.0 (latest stable)
- **MCP Protocol Version**: 2025-03-26

### 2. Agentic AI: Python LangGraph Orchestrator
- **Location**: New directory `orchestrator/` at project root
- **Port**: 8090 (FastAPI)
- **Framework**: LangGraph + LangChain
- **LLM Provider**: Groq (matches project constraint)
- **MCP Client**: `langchain-mcp-adapters` to connect to Java MCP servers
- **Python Version**: 3.11+

### 3. MCP Tools

#### order-service Tools
**Tool 1: `check_order_status`**
- **Input**: `orderId` (string, UUID format)
- **Output**: Order status (PENDING, CONFIRMED, SHIPPED, DELIVERED), total amount, item count
- **Implementation**: Wraps existing `OrderService.getOrder()` method
- **Purpose**: Chatbot answers "What's the status of order X?"

**Tool 2: `create_b2b_order`**
- **Input**: `accountId` (string, UUID), `items` (array of {sku: string, quantity: int})
- **Output**: Order ID, status, total amount
- **Implementation**: Wraps existing `OrderService.createOrder()` method
- **Purpose**: Chatbot creates orders autonomously (full autonomy per project constraint)

#### catalog-service Tools
**Tool 3: `search_products`**
- **Input**: `query` (string), `category` (optional string)
- **Output**: List of products with SKU, name, price, inventory
- **Implementation**: Wraps existing `CatalogService.getProducts()` method
- **Purpose**: Chatbot helps users find products

**Tool 4: `get_product_by_sku`**
- **Input**: `sku` (string)
- **Output**: Product details with tier pricing
- **Implementation**: Wraps existing `CatalogService.getProductBySku()` method
- **Purpose**: Chatbot provides detailed product info

### 4. Implementation Approach

#### order-service Changes
1. Add Spring AI MCP starter dependency to `pom.xml`
2. Create `McpServerConfig.java` to register tools
3. Create `OrderMcpTools.java` with `@Tool` annotated methods wrapping `OrderService`
4. Update `application.yml` with MCP server config
5. Expose SSE endpoint at `/sse`

#### catalog-service Changes
1. Add Spring AI MCP starter dependency to `pom.xml`
2. Create `McpServerConfig.java` to register tools
3. Create `CatalogMcpTools.java` with `@Tool` annotated methods wrapping `CatalogService`
4. Update `application.yml` with MCP server config
5. Expose SSE endpoint at `/sse`

### 5. Python Orchestrator Structure
```
orchestrator/
├── requirements.txt
├── main.py                               # FastAPI entry point
├── agent.py                              # LangGraph agent definition
├── mcp_client.py                         # MCP client connecting to Java servers
├── tools.py                              # Tool wrappers
├── prompts.py                            # System prompts
└── Dockerfile.orchestrator
```

### 6. Docker Compose Updates
- Add `orchestrator` service (port 8090)
- `orchestrator` depends on `order-service` and `catalog-service`
- No new Java services needed

### 7. Gateway Routing
Add route in `gateway-service`:
```yaml
- id: orchestrator
  uri: http://orchestrator:8090
  predicates:
    - Path=/api/chat/**
  filters:
    - StripPrefix=0
```

### 8. Angular Chat UI (Phase 4 preview)
- New route: `/chat`
- Chat component with message bubbles
- REST connection to Python orchestrator
- Full autonomy: no human approval needed for order creation

## Implementation Order

### Wave 1: MCP Tools in order-service
1. Add Spring AI MCP starter to `pom.xml`
2. Create `OrderMcpTools.java` with `@Tool` methods
3. Create `McpServerConfig.java`
4. Update `application.yml`
5. Test MCP server with MCP Inspector

### Wave 2: MCP Tools in catalog-service
1. Add Spring AI MCP starter to `pom.xml`
2. Create `CatalogMcpTools.java` with `@Tool` methods
3. Create `McpServerConfig.java`
4. Update `application.yml`
5. Test MCP server with MCP Inspector

### Wave 3: Python Orchestrator
1. Create FastAPI project with LangGraph
2. Configure Groq LLM
3. Set up MCP clients to connect to both Java servers
4. Define agent graph with tool-calling nodes
5. Test agent with sample queries

### Wave 4: Integration
1. Update docker-compose.yml
2. Update gateway routes
3. End-to-end test: Angular → Orchestrator → MCP → Services

## Key Constraints
- **Full autonomy**: Chatbot can execute orders without human approval
- **Groq LLM**: Use `mixtral-8x7b-32768` or `llama-3.3-70b-versatile`
- **No Eureka**: Services use direct HTTP (localhost for dev, container names for Docker)
- **JWT Auth**: MCP tools don't require auth (internal service-to-service)
- **Mock product data**: order-service still uses mock data (not calling catalog-service)

## Risks & Mitigations
| Risk | Mitigation |
|------|-----------|
| Spring AI MCP starter version compatibility | Use Spring AI 1.1.0 with Spring Boot 3.5.15 |
| Python orchestrator port conflict | Use port 8090 (not 8080-8084) |
| MCP SSE connection drops | Implement reconnection logic in Python client |
| LangGraph state management | Use simple in-memory state for MVP |

## Success Criteria (from ROADMAP)
1. ✅ MCP tool `check_order_status(orderId)` returns order status from database
2. ✅ MCP tool `create_b2b_order(accountId, items)` creates new order and returns order ID
3. ✅ MCP Server is accessible via HTTP/SSE transport

## References
- **Order API**: `GET /api/orders/{id}`, `POST /api/orders`
- **Order DTOs**: `OrderResponse`, `CreateOrderRequest`, `OrderItemRequest`
- **Order Statuses**: PENDING → CONFIRMED → SHIPPED → DELIVERED
- **Catalog SKUs**: BRK-001..005, ELC-001..005, ENG-001..005, FLT-001..002, SUS-001..003
