# B2B Auto Parts Platform - Operations Guide

## Architecture Overview

```
localhost:4200  → Angular Frontend (ng serve)
localhost:8080  → Spring Cloud Gateway (Docker)
localhost:8081  → Account Service (Docker)
localhost:8082  → Catalog Service + MCP (Docker)
localhost:8083  → Order Service + MCP (Docker)
localhost:8090  → Python Chatbot Orchestrator (local)
localhost:5432  → Account DB (Docker)
localhost:5433  → Catalog DB (Docker)
localhost:5434  → Order DB (Docker)
localhost:5672  → RabbitMQ (Docker)
localhost:15672 → RabbitMQ Management UI (Docker)
```

---

## Start All Services

Run from project root `C:\Veera\opencodews\practice\ecommerce-btob`:

```powershell
# 1. Start Docker services (DBs, RabbitMQ, Java microservices)
docker-compose up -d

# 2. Wait for health checks (30-60 seconds)
docker-compose ps

# 3. Start Python Chatbot
Start-Process -FilePath "C:\Veera\opencodews\practice\ecommerce-btob\chatbot-agents\venv\Scripts\python.exe" `
  -ArgumentList "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8090", "--log-level", "info" `
  -WorkingDirectory "C:\Veera\opencodews\practice\ecommerce-btob\chatbot-agents" `
  -WindowStyle Hidden

# 4. Start Angular Frontend
cd C:\Veera\opencodews\practice\ecommerce-btob\frontend
npm start
```

## Stop All Services

```powershell
# 1. Stop Angular (Ctrl+C in its terminal)

# 2. Stop Python Chatbot
$pid = (netstat -ano | Select-String ":8090" | Select-String "LISTENING" | ForEach-Object { ($_ -split '\s+')[-1] } | Select-Object -First 1)
if ($pid) { Stop-Process -Id $pid -Force }

# 3. Stop Docker services
docker-compose down
```

---

## Individual Service Commands

### Docker (Infrastructure + Java Services)

```powershell
cd C:\Veera\opencodews\practice\ecommerce-btob

# Start everything
docker-compose up -d

# Stop everything
docker-compose down

# Restart everything
docker-compose down && docker-compose up -d

# View status
docker-compose ps

# View logs (all services)
docker-compose logs -f

# View logs (specific service)
docker-compose logs -f account-service
docker-compose logs -f catalog-service
docker-compose logs -f order-service
docker-compose logs -f gateway-service

# Rebuild and start (after code changes)
docker-compose up -d --build

# Rebuild single service
docker-compose up -d --build account-service
docker-compose up -d --build catalog-service
docker-compose up -d --build order-service
docker-compose up -d --build gateway-service
```

### Python Chatbot (Local)

```powershell
# Start
Start-Process -FilePath "C:\Veera\opencodews\practice\ecommerce-btob\chatbot-agents\venv\Scripts\python.exe" `
  -ArgumentList "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8090", "--log-level", "info" `
  -WorkingDirectory "C:\Veera\opencodews\practice\ecommerce-btob\chatbot-agents" `
  -WindowStyle Hidden

# Stop
$pid = (netstat -ano | Select-String ":8090" | Select-String "LISTENING" | ForEach-Object { ($_ -split '\s+')[-1] } | Select-Object -First 1)
if ($pid) { Stop-Process -Id $pid -Force }

# Check health
Invoke-WebRequest -Uri "http://localhost:8090/health" -UseBasicParsing | Select-Object -ExpandProperty Content

# Run tests
cd C:\Veera\opencodews\practice\ecommerce-btob
& "chatbot-agents\venv\Scripts\python.exe" -m pytest chatbot-agents/tests/ -v
```

### Angular Frontend (Local)

```powershell
# Start
cd C:\Veera\opencodews\practice\ecommerce-btob\frontend
npm start

# Stop
# Press Ctrl+C in the terminal where npm start is running

# Build for production
npm run build
```

---

## Health Checks

```powershell
# All services
docker-compose ps

# Individual health endpoints
Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing   # Gateway
Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -UseBasicParsing   # Account
Invoke-WebRequest -Uri "http://localhost:8082/actuator/health" -UseBasicParsing   # Catalog
Invoke-WebRequest -Uri "http://localhost:8083/actuator/health" -UseBasicParsing   # Order
Invoke-WebRequest -Uri "http://localhost:8090/health" -UseBasicParsing            # Chatbot
```

---

## Ports Reference

| Port  | Service              | Protocol | Notes                     |
|-------|----------------------|----------|---------------------------|
| 4200  | Angular Frontend     | HTTP     | Local dev server          |
| 8080  | API Gateway          | HTTP     | Routes to all services    |
| 8081  | Account Service      | HTTP     | JWT auth, accounts        |
| 8082  | Catalog Service+MCP  | HTTP/SSE | Products, MCP tools       |
| 8083  | Order Service+MCP    | HTTP/SSE | Orders, MCP tools         |
| 8090  | Python Chatbot       | HTTP     | LangGraph orchestrator    |
| 5432  | Account DB           | PostgreSQL| account_db               |
| 5433  | Catalog DB           | PostgreSQL| catalog_db               |
| 5434  | Order DB             | PostgreSQL| order_db                 |
| 5672  | RabbitMQ             | AMQP     | Event messaging           |
| 15672 | RabbitMQ Management  | HTTP     | UI: guest/guest           |

---

## Quick One-Liners

```powershell
# Full startup (Docker + Chatbot)
docker-compose up -d; Start-Sleep 30; Start-Process -FilePath "C:\Veera\opencodews\practice\ecommerce-btob\chatbot-agents\venv\Scripts\python.exe" -ArgumentList "-m","uvicorn","main:app","--host","0.0.0.0","--port","8090","--log-level","info" -WorkingDirectory "C:\Veera\opencodews\practice\ecommerce-btob\chatbot-agents" -WindowStyle Hidden

# Full shutdown
$pid = (netstat -ano | Select-String ":8090" | Select-String "LISTENING" | ForEach-Object { ($_ -split '\s+')[-1] } | Select-Object -First 1); if ($pid) { Stop-Process -Id $pid -Force }; docker-compose down

# Check everything is running
docker-compose ps; Invoke-WebRequest -Uri "http://localhost:8090/health" -UseBasicParsing | Select-Object -ExpandProperty Content; netstat -ano | Select-String "LISTENING" | Select-String ":(4200|8080|8081|8082|8083|8090)"
```

---

## Troubleshooting

```powershell
# Port already in use
netstat -ano | Select-String ":8090"    # Find PID
Stop-Process -Id <PID> -Force           # Kill it

# Docker container won't start
docker-compose logs <service-name>      # Check logs

# Chatbot won't connect to MCP servers
# 1. Check Java services are healthy
docker-compose ps
# 2. Restart chatbot
# 3. Check chatbot-agents/chatbot.log

# Angular proxy errors
# Ensure gateway is running on 8080 first
```
