# Spring Cloud Interview Cheat Sheet

---

## Spring Cloud

### What is Spring Cloud?
Framework for building distributed systems and microservices. Provides solutions for common microservice challenges.

### Key Components

| Component | Purpose | Example |
|-----------|---------|---------|
| Spring Cloud Config | Centralized configuration server | All microservices read config from one place |
| Eureka (Service Discovery) | Services register and discover each other | No hardcoded URLs |
| Spring Cloud Gateway | API Gateway — single entry point | Route, filter, authenticate requests |
| Feign Client | Declarative HTTP client | Call other microservices like Java methods |
| Resilience4j | Circuit breaker, retry, rate limiter | Handle downstream failures |
| Spring Cloud Sleuth | Distributed tracing | Trace requests across microservices |

---

### Spring Cloud Config

```yaml
# config-server application.yml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/my-org/config-repo
```

```yaml
# microservice bootstrap.yml — points to config server
spring:
  application:
    name: finance-service
  cloud:
    config:
      uri: http://config-server:8888
```

**Why:** Instead of each microservice having its own application.yml, all config lives in one Git repo. Change config without redeploying services.

---

### Feign Client

```java
// Declarative HTTP client — no RestTemplate boilerplate
@FeignClient(name = "reservation-service", url = "${reservation.service.url}")
public interface ReservationClient {

    @GetMapping("/reservations/{id}")
    Reservation getReservation(@PathVariable Long id);

    @PostMapping("/reservations/bulk")
    List<Reservation> getReservations(@RequestBody Set<Long> ids);
}

// Usage — feels like calling a local method
@Service
public class ReminderService {
    private final ReservationClient reservationClient;

    public void generateReminders() {
        List<Reservation> reservations = reservationClient.getReservations(ids);
    }
}
```

---

### Circuit Breaker — Resilience4j

**Problem:** Service A calls Service B. Service B is down. Without circuit breaker, Service A keeps waiting → thread pool exhausted → Service A also goes down (cascading failure).

**Circuit breaker states:**
```
CLOSED  → requests pass through normally
OPEN    → requests blocked, fallback returned immediately
HALF_OPEN → limited requests allowed to test if service recovered
```

```java
@CircuitBreaker(name = "reservationService", fallbackMethod = "fallbackReservations")
@Retry(name = "reservationService")
public List<Reservation> getReservations(Set<Long> ids) {
    return reservationClient.getReservations(ids);
}

// Fallback — called when circuit is OPEN
public List<Reservation> fallbackReservations(Set<Long> ids, Exception e) {
    log.error("Reservation service unavailable, returning empty list", e);
    return Collections.emptyList();
}
```

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      reservationService:
        sliding-window-size: 10
        failure-rate-threshold: 50      # open if 50% requests fail
        wait-duration-in-open-state: 10s
        permitted-calls-in-half-open-state: 3
```

---

### API Gateway — Spring Cloud Gateway

```java
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("finance-service", r -> r
                .path("/api/finance/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Service", "finance")
                )
                .uri("lb://finance-service"))  // lb = load balanced via Eureka
            .route("reminder-service", r -> r
                .path("/api/reminders/**")
                .uri("lb://reminder-service"))
            .build();
    }
}
```

**Why API Gateway:**
- Single entry point for all clients
- Authentication/authorization in one place
- Rate limiting
- Load balancing
- SSL termination

---

### Service Discovery — Eureka

```java
// Server
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {}

// Client (each microservice)
@SpringBootApplication
@EnableEurekaClient
public class FinanceServiceApplication {}
```

```yaml
# microservice application.yml
eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka
```

**How it works:**
```
1. Each microservice registers with Eureka on startup
2. Eureka maintains registry of all running instances
3. Services discover each other by name, not IP
4. If instance goes down, Eureka removes it from registry
```

---


---

## Spring Cloud — Deep Dive Q&A

### Q1: Multiple instances registered in Eureka — how does API Gateway know which to call?

**Answer: Client-side Load Balancing via Spring Cloud LoadBalancer**

```
Eureka Registry:
finance-service →
    Instance 1: 10.0.0.1:8080
    Instance 2: 10.0.0.2:8080
    Instance 3: 10.0.0.3:8080

API Gateway receives: GET /api/finance/payments
        ↓
Route config: uri: lb://finance-service
        ↓
Spring Cloud LoadBalancer
        ↓
Fetches instances from local Eureka cache
        ↓
Picks instance using Round Robin:
Request 1 → Instance 1
Request 2 → Instance 2
Request 3 → Instance 3
Request 4 → Instance 1 (repeats)
```

**Gateway route config:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: finance-service
          uri: lb://finance-service   # lb = load balanced
          predicates:
            - Path=/api/finance/**
```

**`lb://` tells gateway:**
> Don't use a hardcoded URL — look up `finance-service` in Eureka registry and load balance across available instances.

**Load balancing strategies:**
| Strategy | How | Default? |
|----------|-----|---------|
| Round Robin | Each instance in turn | Yes |
| Random | Random instance | No |
| Weighted | Based on instance weight | No |

**Interview answer:**
> "API Gateway uses Spring Cloud LoadBalancer with `lb://service-name` in route config. When a request comes in, LoadBalancer fetches available instances from the local Eureka registry cache and picks one using Round Robin by default. The cache is refreshed every 30 seconds — so new instances become available within 30 seconds of registration."

---

### Q2: How is a service instance registered with a pod in Kubernetes?

**This is the Eureka + Kubernetes integration question.**

**Without Kubernetes — plain Eureka:**
```
Instance starts → registers IP:PORT with Eureka
Eureka stores: finance-service → 10.0.0.1:8080
```

**With Kubernetes — two approaches:**

**Approach 1: Eureka inside Kubernetes**
```
Pod starts (IP assigned by K8s e.g. 10.244.1.5)
        ↓
Spring Boot app inside pod starts
        ↓
Eureka client reads pod IP automatically
        ↓
Registers: finance-service → 10.244.1.5:8080 with Eureka
        ↓
API Gateway routes to pod IP directly
```

```yaml
# Each pod registers its own IP
eureka:
  instance:
    prefer-ip-address: true      # use pod IP not hostname
    ip-address: ${POD_IP}        # injected by K8s as env variable
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka
```

```yaml
# Kubernetes deployment — inject pod IP
env:
  - name: POD_IP
    valueFrom:
      fieldRef:
        fieldPath: status.podIP
```

**Approach 2: Skip Eureka, use Kubernetes Service Discovery**
```
K8s Service → handles load balancing natively
finance-service.namespace.svc.cluster.local → resolves to pod IPs
No Eureka needed
```

**Most modern setups with K8s use Approach 2 — Kubernetes Service replaces Eureka.**

**Interview answer:**
> "Each pod registers itself with Eureka using its pod IP, which Kubernetes injects as an environment variable. The Eureka client inside the pod reads this IP and registers it with the Eureka server on startup. When the pod is terminated, it deregisters — or Eureka removes it after missing heartbeats. In modern Kubernetes setups, Eureka is often replaced entirely by Kubernetes Service discovery — K8s Services handle load balancing natively via DNS."

---

### Q3: If centralized config is changed, how do services get updated config?

**Three approaches:**

**Approach 1: Manual restart (simplest, not ideal)**
```
Change config in Git
        ↓
Restart microservice
        ↓
Service reads new config from Config Server on startup
```

**Approach 2: @RefreshScope + /actuator/refresh (manual trigger)**
```java
// Mark bean as refreshable
@RefreshScope
@RestController
public class PaymentController {
    @Value("${payment.timeout}")
    private int timeout;  // updates without restart
}
```

```bash
# Trigger refresh manually via HTTP POST
POST http://finance-service/actuator/refresh

# Service fetches new config from Config Server
# @RefreshScope beans re-initialized with new values
```

**Approach 3: Spring Cloud Bus + automatic refresh (production standard)**
```
Config changed in Git
        ↓
Webhook triggers Config Server
        ↓
Config Server publishes event to Spring Cloud Bus (RabbitMQ/Kafka)
        ↓
ALL microservices receive event simultaneously
        ↓
Each service calls /actuator/refresh internally
        ↓
@RefreshScope beans updated — no restart needed
```

```yaml
# application.yml
spring:
  cloud:
    bus:
      enabled: true
  rabbitmq:
    host: rabbitmq
    port: 5672
```

**Flow diagram:**
```
Git repo (config changed)
        ↓ webhook
Config Server
        ↓ publishes RefreshEvent
RabbitMQ/Kafka (Spring Cloud Bus)
        ↓ broadcasts
Finance Service    Settlement Service    Reminder Service
(auto refreshed)   (auto refreshed)     (auto refreshed)
```

**Interview answer:**
> "By default, config changes require a restart. For dynamic refresh without restart, we use @RefreshScope on beans and Spring Cloud Bus. When config changes in Git, a webhook notifies Config Server which publishes a refresh event to Spring Cloud Bus — backed by RabbitMQ or Kafka. All microservices subscribed to the bus receive the event and refresh their @RefreshScope beans automatically. This allows config changes to propagate across all instances without any restart."

---

### Q4: How to make sure Config Server is always running?

**Multiple strategies — layered approach:**

**Strategy 1: Run multiple Config Server instances**
```
Config Server Instance 1 (Broker 1)
Config Server Instance 2 (Broker 2)
        ↓
Load Balancer
        ↓
Microservices connect via load balancer URL
```

```yaml
# Microservice config
spring:
  cloud:
    config:
      uri: http://config-server-lb:8888  # load balancer URL
```

**Strategy 2: Config Server in Kubernetes with multiple replicas**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: config-server
spec:
  replicas: 3      # 3 instances always running
  template:
    spec:
      containers:
      - name: config-server
        image: config-server:1.0
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8888
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8888
```

**Strategy 3: Client-side retry + fail-fast**
```yaml
# Microservice config
spring:
  cloud:
    config:
      fail-fast: true        # fail startup if config server unreachable
      retry:
        max-attempts: 6
        initial-interval: 1000
        multiplier: 1.5
        max-interval: 2000
```

**Strategy 4: Local config fallback**
```yaml
# Microservice — use local config if config server down
spring:
  cloud:
    config:
      fail-fast: false       # don't fail — use local application.yml
```

**Strategy 5: Config client cache**
- Spring Cloud Config client caches fetched config locally
- On restart, if Config Server down → uses cached config
- Config stays available even if Config Server temporarily down

**What happens in each failure scenario:**

| Scenario | Impact | Solution |
|----------|--------|----------|
| Config Server down, services already running | No impact — config in memory | HA Config Server |
| Config Server down, new service starting | Service fails to start | Retry + local fallback |
| Config Server down, config changed | Change not propagated | Spring Cloud Bus + HA |
| All Config Server instances down | New instances can't start | K8s replicas + PodDisruptionBudget |

**Interview answer:**
> "Config Server HA is achieved by running multiple instances behind a load balancer — in Kubernetes, we set replicas to 3 and use a Kubernetes Service in front. Config Server itself is stateless — config lives in Git, not in Config Server memory, so scaling is straightforward. On the client side, we configure retry with exponential backoff so transient Config Server unavailability doesn't cause startup failures. Already running services are not affected — they have config in memory. For truly critical environments, we also configure local config fallback so services can start with default config if Config Server is unreachable."

---

### Q5: Authentication at API Gateway level

**JWT-based authentication — standard pattern:**

```
Client → Bearer JWT token in Authorization header
        ↓
API Gateway — Global Filter (runs before routing)
        ↓
Extract JWT from header
Validate signature + expiry
        ↓
Invalid → return 401 immediately (downstream never called)
        ↓
Valid → extract userId, roles from JWT claims
Add as request headers: X-User-Id, X-User-Role
        ↓
Forward to downstream microservice
        ↓
Downstream trusts headers — no re-validation
```

**Global Filter implementation:**
```java
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // Public endpoints — skip auth
    private final List<String> publicPaths = List.of(
        "/api/auth/login",
        "/api/auth/register",
        "/actuator/health"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        // Skip auth for public paths
        if (publicPaths.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
            .getHeaders()
            .getFirst(HttpHeaders.AUTHORIZATION);

        // Missing or invalid format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.validateAndExtract(token);

            // Inject user info as headers for downstream services
            ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Role", claims.get("role", String.class))
                .header("X-Tenant-Id", claims.get("tenantId", String.class))
                .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        } catch (JwtException e) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;  // run before other filters
    }
}
```

**Interview answer:**
> "Authentication is centralized at the API Gateway via a Global Filter that intercepts every request before routing. It extracts the JWT from the Authorization header, validates signature and expiry. Invalid tokens return 401 immediately — downstream services are never called. Valid tokens have userId and roles extracted from claims and injected as X-User-Id and X-User-Role headers. Downstream microservices trust these headers without re-validating the token — authentication logic stays in one place. Public endpoints like /auth/login are whitelisted to bypass the filter."