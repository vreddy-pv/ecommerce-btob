# Phase 3: MCP Server Integration — PLAN.md

## Goal
Add MCP server capabilities to existing order-service and catalog-service, expose service methods as AI tools, and create Python LangGraph orchestrator for agentic AI.

## Dependencies
- Phase 1 (Backend Foundation) — order-service and catalog-service REST APIs
- Phase 2 (Angular Frontend) — not required for this phase

## Requirements
- CHAT-01: MCP tool `check_order_status(orderId)` returns order status
- CHAT-02: MCP tool `create_b2b_order(accountId, items)` creates order

## Plans

### Plan 03-01: MCP Tools in order-service
**Goal**: Add Spring AI MCP server to order-service, expose order operations as tools

**Tasks**:
1. Update `order-service/pom.xml`
   - Add `spring-ai-mcp-server-webmvc-spring-boot-starter` dependency
   - Add `spring-ai-bom` to dependencyManagement

2. Create `OrderMcpTools.java`
   - `@Tool("check_order_status")` — wraps `OrderService.getOrder()`
   - `@Tool("create_b2b_order")` — wraps `OrderService.createOrder()`
   - Input/output DTOs for tool parameters

3. Create `McpServerConfig.java`
   - Register `OrderMcpTools` as `ToolCallbackProvider`

4. Update `order-service/application.yml`
   - Add `spring.ai.mcp.server` config (name, version, type)

5. Test MCP server
   - Start order-service
   - Verify SSE endpoint at `/sse`
   - Test tool discovery

**Success Criteria**:
- [ ] `mvn clean install` passes
- [ ] MCP server starts on port 8083 (same as REST API)
- [ ] SSE endpoint `/sse` is accessible
- [ ] Tools `check_order_status` and `create_b2b_order` are registered

---

### Plan 03-02: MCP Tools in catalog-service
**Goal**: Add Spring AI MCP server to catalog-service, expose catalog operations as tools

**Tasks**:
1. Update `catalog-service/pom.xml`
   - Add `spring-ai-mcp-server-webmvc-spring-boot-starter` dependency
   - Add `spring-ai-bom` to dependencyManagement

2. Create `CatalogMcpTools.java`
   - `@Tool("search_products")` — wraps `CatalogService.getProducts()`
   - `@Tool("get_product_by_sku")` — wraps `CatalogService.getProductBySku()`
   - Input/output DTOs for tool parameters

3. Create `McpServerConfig.java`
   - Register `CatalogMcpTools` as `ToolCallbackProvider`

4. Update `catalog-service/application.yml`
   - Add `spring.ai.mcp.server` config (name, version, type)

5. Test MCP server
   - Start catalog-service
   - Verify SSE endpoint at `/sse`
   - Test tool discovery

**Success Criteria**:
- [ ] `mvn clean install` passes
- [ ] MCP server starts on port 8082 (same as REST API)
- [ ] SSE endpoint `/sse` is accessible
- [ ] Tools `search_products` and `get_product_by_sku` are registered

---

### Plan 03-03: Python LangGraph Orchestrator
**Goal**: Create Python FastAPI service with LangGraph agent that calls MCP tools from both services

**Tasks**:
1. Create `orchestrator/` directory
   - `requirements.txt` (fastapi, langgraph, langchain, langchain-mcp-adapters, groq)
   - `main.py` (FastAPI entry point)

2. Implement MCP Clients
   - `mcp_client.py` — connects to order-service and catalog-service MCP servers via SSE
   - Tool discovery and invocation

3. Implement LangGraph Agent
   - `agent.py` — define graph with LLM node + tool node
   - System prompt for B2B auto parts assistant
   - Full autonomy mode (no human approval)

4. Implement FastAPI Endpoints
   - `POST /chat` — send message, get response
   - `GET /health` — health check
   - WebSocket or SSE for streaming responses

5. Create `Dockerfile.orchestrator`
   - Python 3.11 base image
   - Install dependencies
   - Run FastAPI with uvicorn

**Success Criteria**:
- [ ] `pip install -r requirements.txt` succeeds
- [ ] FastAPI starts on port 8090
- [ ] Agent can call `check_order_status` tool
- [ ] Agent can call `create_b2b_order` tool
- [ ] Agent can call `search_products` tool
- [ ] End-to-end: message → LLM → tool call → response

---

### Plan 03-04: Integration & Testing
**Goal**: Wire all services together, test end-to-end flow

**Tasks**:
1. Update `docker-compose.yml`
   - Add `orchestrator` service (port 8090)
   - `orchestrator` depends on `order-service` and `catalog-service`

2. Update `gateway-service/application.yml`
   - Add route for `/api/chat/**` → orchestrator:8090

3. Start all services via Docker Compose
   - `docker-compose up --build`
   - Verify all 6 services healthy

4. Test MCP Servers
   - Use MCP Inspector or curl to test SSE endpoints
   - Verify tool discovery on both services

5. Test Python Orchestrator
   - Send test message: "What's the status of order {id}?"
   - Verify tool is called and response is returned

6. Test Product Search
   - Send message: "Find brake pads"
   - Verify catalog search tool is called

7. Test Order Creation
   - Send message: "Create an order for account {id} with 2x BRK-001"
   - Verify order is created in database

8. Update ROADMAP.md
   - Mark Phase 3 as complete
   - Update progress table

**Success Criteria**:
- [ ] All services start without errors
- [ ] Chatbot can check order status
- [ ] Chatbot can search products
- [ ] Chatbot can create orders autonomously
- [ ] Orders appear in Angular orders view

## Execution Order
03-01 → 03-02 → 03-03 → 03-04 (sequential, no parallelization)

## Estimated Effort
- Plan 03-01: ~1 hour
- Plan 03-02: ~1 hour
- Plan 03-03: ~2 hours
- Plan 03-04: ~1 hour
- **Total**: ~5 hours
