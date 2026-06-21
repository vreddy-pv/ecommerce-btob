# Phase 1: Backend Foundation - Research

**Researched:** 2026-06-21
**Domain:** Spring Boot 3.x Microservices, B2B E-commerce Backend
**Confidence:** HIGH

## Summary

This research covers the technical foundation for building a B2B auto parts e-commerce platform using Spring Boot 3.x microservices architecture. The platform requires three independent services (account-service, catalog-service, order-service) with database-per-service pattern, Spring Cloud Stream for event-driven messaging, Spring Cloud Gateway for API routing, and Eureka for service discovery.

**Primary recommendation:** Use Spring Boot 3.5.x (latest stable 3.x) with Java 17+, Spring Cloud Gateway, Eureka, and PostgreSQL for the backend foundation.

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUTH-01 | B2B user can log in with email/password and receive JWT token | JWT authentication pattern with Spring Security 6.x |
| AUTH-02 | JWT token persists across browser refresh for session management | Refresh token pattern implementation |
| AUTH-03 | B2B accounts have API keys for service-to-service authentication | API key validation filter pattern |
| CATALOG-01 | Auto parts catalog displays items with SKU, name, description, price, and inventory level | JPA entity design with Spring Data |
| CATALOG-02 | Users can search and filter parts by category, name, or SKU | QueryDSL or JPA Specifications pattern |
| CATALOG-03 | B2B pricing tiers display different prices based on account level | Tier-based pricing entity relationship |
| ORDER-01 | Users can create new B2B orders with line items (SKU + quantity) | Order/OrderItem aggregate pattern |
| ORDER-02 | Order status tracks PENDING, SHIPPED, DELIVERED states | State machine or enum-based status |
| ORDER-03 | Users can view order history and check order status | Pagination pattern with Spring Data |
| ACCT-01 | B2B accounts have credit limits and current balance tracking | Account balance entity design |
| ACCT-02 | Account tier determines pricing level and credit terms | Account tier enum relationship |

## Technical Findings

### 1. Spring Boot 3.x Microservices Best Practices

**Standard Stack:**
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.5.x (latest: 3.5.15) | Core framework | Latest stable 3.x, Java 17+ support |
| Spring Framework | 6.2.x | Core framework | Required by Spring Boot 3.5.x |
| Spring Security | 6.x | Authentication/Authorization | JWT, OAuth2 support |
| Spring Data JPA | 3.x | Database access | Repository pattern |
| Spring Cloud | 2024.0.x | Microservices tools | Gateway, Eureka, Stream |
| Spring Cloud Gateway | 4.x | API Gateway | Reactive, non-blocking |
| Spring Cloud Netflix Eureka | 4.x | Service Discovery | Standard service registry |
| Spring Cloud Stream | 4.x | Event-driven messaging | Kafka/RabbitMQ abstraction |
| PostgreSQL | 15+ | Production database | ACID, JSON support |
| Lombok | Latest | Boilerplate reduction | Getters, setters, builders |
| MapStruct | 1.5+ | Object mapping | Entity-DTO conversion |
| SpringDoc OpenAPI | 2.x | API documentation | Swagger UI generation |

**Key Patterns:**
- **Domain-Driven Service Boundaries:** Services around business domains (account, catalog, order), not technical concerns
- **Database-per-Service:** Each microservice owns its data, never directly accesses another's database
- **Event-Driven Communication:** Use Spring Cloud Stream for async messaging between services
- **API Gateway:** Single entry point with routing, auth, rate limiting

### 2. Database-per-Service Pattern

**Implementation:**
- Each service has its own PostgreSQL database instance
- Services communicate via events (order creation, inventory changes)
- No shared databases or cross-service joins
- Eventual consistency is acceptable for B2B e-commerce

**Database Schema Design (per service):**

**Account Service:**
```sql
CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    tier VARCHAR(50) NOT NULL, -- STANDARD, SILVER, GOLD, PLATINUM
    credit_limit DECIMAL(12,2),
    current_balance DECIMAL(12,2),
    api_key VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Catalog Service:**
```sql
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_id UUID REFERENCES categories(id),
    sort_order INTEGER DEFAULT 0
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    sku VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    base_price DECIMAL(10,2) NOT NULL,
    inventory_level INTEGER DEFAULT 0,
    category_id UUID REFERENCES categories(id),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tier_pricing (
    id UUID PRIMARY KEY,
    product_id UUID REFERENCES products(id),
    tier VARCHAR(50) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    UNIQUE(product_id, tier)
);
```

**Order Service:**
```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL, -- PENDING, CONFIRMED, SHIPPED, DELIVERED
    total_amount DECIMAL(12,2),
    credit_used DECIMAL(12,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID REFERENCES orders(id),
    product_sku VARCHAR(100) NOT NULL,
    product_name VARCHAR(255),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL
);
```

### 3. Spring Cloud Stream for Event-Driven Messaging

**Events to Implement:**
- `OrderCreated` - Emitted when order is created, consumed by catalog (inventory update)
- `OrderStatusChanged` - Emitted when order status changes
- `InventoryUpdated` - Emitted when inventory level changes
- `AccountUpdated` - Emitted when account balance/tier changes

**Configuration (application.yml):**
```yaml
spring:
  cloud:
    stream:
      bindings:
        orderCreated-out-0:
          destination: order-events
          content-type: application/json
        orderCreated-in-0:
          destination: order-events
          group: catalog-service
      rabbit:
        binder:
          addresses: localhost:5672
```

**Producer Pattern:**
```java
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {
    private final StreamBridge streamBridge;
    
    public void publishOrderCreated(OrderCreatedEvent event) {
        streamBridge.send("orderCreated-out-0", event);
    }
}
```

**Consumer Pattern:**
```java
@Component
public class OrderEventConsumer {
    @Bean
    public Consumer<OrderCreatedEvent> orderCreated() {
        return event -> {
            // Update inventory
        };
    }
}
```

### 4. Spring Cloud Gateway and Eureka Setup

**Gateway Configuration:**
```yaml
spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        - id: account-service
          uri: lb://ACCOUNT-SERVICE
          predicates:
            - Path=/api/accounts/**
          filters:
            - StripPrefix=1
        - id: catalog-service
          uri: lb://CATALOG-SERVICE
          predicates:
            - Path=/api/catalog/**
          filters:
            - StripPrefix=1
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=1
    discovery:
      client:
        service-url:
          defaultZone: http://localhost:8761/eureka/
```

**Eureka Server Configuration:**
```yaml
server:
  port: 8761
spring:
  application:
    name: eureka-server
eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

**Service Client Configuration (each microservice):**
```yaml
spring:
  application:
    name: account-service
  cloud:
    discovery:
      enabled: true
      service-url:
        defaultZone: http://localhost:8761/eureka/
```

### 5. JWT Authentication with Spring Security

**JWT Authentication Flow:**
1. User sends credentials to `/api/auth/login`
2. Server validates credentials, generates JWT
3. Client stores JWT (localStorage or cookie)
4. Client sends JWT in `Authorization: Bearer <token>` header
5. Gateway validates JWT before routing to services

**Security Configuration:**
```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/catalog/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), 
                UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
}
```

**JWT Filter Pattern:**
```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        
        if (token != null && tokenProvider.validateToken(token)) {
            Authentication auth = tokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

**API Key Authentication (service-to-service):**
```java
@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("X-API-Key");
        
        if (apiKey != null && apiKeyService.validateApiKey(apiKey)) {
            // Authenticate as service account
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### 6. B2B E-commerce Domain Modeling

**Key Design Decisions:**
- Store prices as `DECIMAL(10,2)` for currency
- Use UUIDs for primary keys (distributed systems friendly)
- Denormalize order items (store product_name, product_sku, unit_price at time of purchase)
- Support tier-based pricing with separate `tier_pricing` table
- Track credit limits and balances on account entity

**Entity Relationships:**
```
Account (1) ---< (N) Order
Order (1) ---< (N) OrderItem
Product (1) ---< (N) TierPricing
Category (1) ---< (N) Product
Category (1) ---< (N) Category (self-referencing for hierarchy)
```

## Architecture Patterns

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway                            │
│               (Spring Cloud Gateway)                        │
│  - Route to services                                        │
│  - JWT validation                                           │
│  - Rate limiting                                            │
└─────────────┬───────────────┬───────────────┬───────────────┘
              │               │               │
              ▼               ▼               ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ Account Service │ │ Catalog Service │ │ Order Service   │
│ - User auth     │ │ - Products      │ │ - Orders        │
│ - JWT tokens    │ │ - Categories    │ │ - Order items   │
│ - API keys      │ │ - Tier pricing  │ │ - Status track  │
│ - Credit limits │ │ - Inventory     │ │ - Credit check  │
└────────┬────────┘ └────────┬────────┘ └────────┬────────┘
         │                   │                   │
         ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ account_db      │ │ catalog_db      │ │ order_db        │
│ (PostgreSQL)    │ │ (PostgreSQL)    │ │ (PostgreSQL)    │
└─────────────────┘ └─────────────────┘ └─────────────────┘
         │                   │                   │
         └───────────────────┼───────────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ RabbitMQ/Kafka  │
                    │ (Events)        │
                    └─────────────────┘
```

### Recommended Project Structure

```
ecommerce-btob/
├── gateway-service/
│   ├── src/main/java/com/btob/gateway/
│   │   ├── GatewayApplication.java
│   │   ├── config/
│   │   │   └── GatewayConfig.java
│   │   └── filter/
│   │       └── JwtAuthenticationFilter.java
│   └── pom.xml
├── account-service/
│   ├── src/main/java/com/btob/account/
│   │   ├── AccountApplication.java
│   │   ├── entity/
│   │   │   ├── Account.java
│   │   │   └── AccountTier.java
│   │   ├── repository/
│   │   │   └── AccountRepository.java
│   │   ├── service/
│   │   │   ├── AccountService.java
│   │   │   └── JwtTokenService.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   └── AccountController.java
│   │   └── dto/
│   │       ├── LoginRequest.java
│   │       ├── AuthResponse.java
│   │       └── AccountDto.java
│   └── pom.xml
├── catalog-service/
│   ├── src/main/java/com/btob/catalog/
│   │   ├── CatalogApplication.java
│   │   ├── entity/
│   │   │   ├── Product.java
│   │   │   ├── Category.java
│   │   │   └── TierPricing.java
│   │   ├── repository/
│   │   │   ├── ProductRepository.java
│   │   │   └── CategoryRepository.java
│   │   ├── service/
│   │   │   └── CatalogService.java
│   │   └── controller/
│   │       └── CatalogController.java
│   └── pom.xml
├── order-service/
│   ├── src/main/java/com/btob/order/
│   │   ├── OrderApplication.java
│   │   ├── entity/
│   │   │   ├── Order.java
│   │   │   ├── OrderItem.java
│   │   │   └── OrderStatus.java
│   │   ├── repository/
│   │   │   └── OrderRepository.java
│   │   ├── service/
│   │   │   └── OrderService.java
│   │   └── controller/
│   │       └── OrderController.java
│   └── pom.xml
└── docker-compose.yml
```

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JWT token generation/validation | Custom crypto code | jjwt library | Security-critical, complex edge cases |
| API documentation | Manual Swagger setup | SpringDoc OpenAPI | Auto-generates from code |
| Database migrations | SQL scripts | Flyway or Liquibase | Version control, rollback support |
| Object mapping | Manual getters/setters | MapStruct or ModelMapper | Type-safe, compile-time generation |
| Service discovery | Custom registry | Eureka | Battle-tested, ecosystem support |
| Event messaging | Custom queue | Spring Cloud Stream | Abstraction, binder support |
| CORS configuration | Manual headers | Spring Security CORS | Integrated with security |

## Common Pitfalls

### Pitfall 1: N+1 Queries in JPA
**What goes wrong:** Loading relationships lazily causes excessive database queries
**Why it happens:** Default `FetchType.LAZY` on `@ManyToOne` relationships
**How to avoid:** Use `@EntityGraph` or JPQL `JOIN FETCH` for known query patterns
**Warning signs:** Slow API responses, high database connection usage

### Pitfall 2: JWT Token Storage Security
**What goes wrong:** Storing JWT in localStorage exposes to XSS attacks
**Why it happens:** Convenience over security
**How to avoid:** Use httpOnly cookies or in-memory storage for web apps
**Warning signs:** Security audit findings, token theft incidents

### Pitfall 3: Database-per-Service Data Consistency
**What goes wrong:** Trying to use distributed transactions across services
**Why it happens:** ACID habits from monolithic databases
**How to avoid:** Accept eventual consistency, implement saga pattern
**Warning signs:** Timeout errors, data inconsistencies between services

### Pitfall 4: Gateway as Single Point of Failure
**What goes wrong:** Gateway becomes bottleneck or fails entirely
**Why it happens:** No horizontal scaling or health checks
**How to avoid:** Multiple gateway instances, circuit breakers, load balancing
**Warning signs:** High latency, connection refused errors

### Pitfall 5: Hardcoded Service URLs
**What goes wrong:** Services can't find each other after deployment
**Why it happens:** Using `localhost` URLs instead of service discovery
**How to avoid:** Always use Eureka service names (`lb://SERVICE-NAME`)
**Warning signs:** Connection errors in distributed environment

## References

### Primary Sources
- Spring Boot 3.5.x Documentation: https://spring.io/projects/spring-boot
- Spring Cloud Gateway: https://spring.io/projects/spring-cloud-gateway
- Spring Cloud Stream: https://spring.io/projects/spring-cloud-stream
- Spring Security 6.x: https://spring.io/projects/spring-security
- PostgreSQL Documentation: https://www.postgresql.org/docs/

### Secondary Sources
- Spring Boot Microservices Best Practices: https://katyella.com/blog/spring-boot-microservices-architecture-patterns
- JWT Authentication in Spring Boot 3: https://blog.tericcabrel.com/jwt-authentication-springboot-spring-security/
- E-Commerce Database Schema: https://erflow.io/en/blog/ecommerce-database-schema

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | RabbitMQ is the default messaging broker (can switch to Kafka) | Spring Cloud Stream | May need different Docker image |
| A2 | PostgreSQL 15+ is available for all services | Database | May need schema adjustments |
| A3 | Java 17 is the minimum version | Spring Boot | Need to verify project requirements |
| A4 | Services will run on different ports locally | Local Development | Need docker-compose port mapping |

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 17+ | All services | ✓ | 17 | — |
| Maven 3.6+ | Build | ✓ | 3.9.x | — |
| PostgreSQL 15+ | Database | ✓ | 15.x | H2 for dev |
| RabbitMQ | Messaging | ✓ | 3.x | In-memory for dev |
| Docker | Deployment | ✓ | 24.x | — |

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito |
| Config file | pom.xml (spring-boot-starter-test) |
| Quick run command | `mvn test` |
| Full suite command | `mvn verify` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AUTH-01 | Login returns JWT | unit | `mvn test -Dtest=AuthServiceTest` | ❌ Wave 0 |
| AUTH-02 | JWT persists refresh | unit | `mvn test -Dtest=JwtTokenTest` | ❌ Wave 0 |
| CATALOG-01 | Products display correctly | unit | `mvn test -Dtest=CatalogServiceTest` | ❌ Wave 0 |
| ORDER-01 | Orders create with items | unit | `mvn test -Dtest=OrderServiceTest` | ❌ Wave 0 |
| ACCT-01 | Credit limits tracked | unit | `mvn test -Dtest=AccountServiceTest` | ❌ Wave 0 |

### Wave 0 Gaps
- [ ] Test infrastructure setup (JUnit 5, Mockito, TestContainers)
- [ ] Test database configuration (H2 or TestContainers)
- [ ] Integration test base class

## Metadata

**Confidence breakdown:**
- Standard Stack: HIGH - Spring Boot 3.5.x is current stable, well-documented
- Architecture: HIGH - Database-per-service and event-driven patterns are standard
- Pitfalls: HIGH - Common issues well-documented in Spring community

**Research date:** 2026-06-21
**Valid until:** 2026-07-21 (30 days - Spring ecosystem is stable)
