---
# Capgemini Interview

## LLD — Tic Tac Toe
- Classes: `Board`, `Player`, `Game`
- `Board` — 3x3 grid, `markCell(row, col, symbol)`, `checkWinner()`
- `Player` — name, symbol (X/O)
- `Game` — alternates turns, checks win/draw after each move
- Win check: rows, cols, 2 diagonals

---

## Max Sum Subarray of Size K (Sliding Window)

```java
int maxSum(int[] arr, int k) {
    int sum = 0, max = 0;
    for (int i = 0; i < k; i++) sum += arr[i];
    max = sum;
    for (int i = k; i < arr.length; i++) {
        sum += arr[i] - arr[i - k];
        max = Math.max(max, sum);
    }
    return max;
}
```

---

## Stream — Make Every 3rd Element Its Square

```java
List<Integer> result = IntStream.range(0, nums.size())
    .mapToObj(i -> (i + 1) % 3 == 0 ? nums.get(i) * nums.get(i) : nums.get(i))
    .collect(Collectors.toList());
// [1,2,3,4,5,6] → [1,2,9,4,5,36]
```

---

## API Gateway Routing (Spring Cloud Gateway)

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://ORDER-SERVICE       # lb = load balanced via Eureka
          predicates:
            - Path=/orders/**
          filters:
            - StripPrefix=1
        - id: user-service
          uri: lb://USER-SERVICE
          predicates:
            - Path=/users/**
```

---

## Global Routing / Filters (Gateway)

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - AddRequestHeader=X-Request-Source, gateway
        - AddResponseHeader=X-Response-Time, 200ms
```

```java
// Global filter in code
@Component
public class AuthFilter implements GlobalFilter {
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null) return exchange.getResponse().setComplete(); // reject
        return chain.filter(exchange);
    }
}
```

---

## Circuit Breaker Config (Resilience4j)

**States**
- **CLOSED** — normal, all requests pass through
- **OPEN** — tripped, all requests fail fast (no actual call made) → fallback runs
- **HALF-OPEN** — trial mode after wait duration; allows limited calls to test if service recovered
  - if those succeed → back to CLOSED
  - if they fail → back to OPEN

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      order-service:
        slidingWindowSize: 10                        # last 10 calls tracked
        failureRateThreshold: 50                     # if 5/10 fail → OPEN
        waitDurationInOpenState: 10s                 # stay OPEN for 10s, then go HALF-OPEN
        permittedNumberOfCallsInHalfOpenState: 3     # allow 3 trial calls in HALF-OPEN
```

```java
@CircuitBreaker(name = "order-service", fallbackMethod = "fallback")
public String getOrder(Long id) {
    return restTemplate.getForObject("/orders/" + id, String.class);
}

public String fallback(Long id, Exception e) {
    return "Order service unavailable";
}
```

**When circuit OPENS — how parent microservice should handle it**

1. **Fallback response** — return cached/default data so parent can still respond
```java
public OrderResponse fallback(Long id, Exception e) {
    // return stale cache or default
    return cache.get(id) != null ? cache.get(id) : new OrderResponse("unavailable");
}
```

2. **Propagate gracefully** — don't let one service failure cascade up
   - Parent catches fallback → returns partial response to client (not 500)
   - e.g. user profile loads, but order history section shows "currently unavailable"

3. **Retry with backoff** — before circuit opens, retry a few times
```yaml
resilience4j:
  retry:
    instances:
      order-service:
        maxAttempts: 3
        waitDuration: 500ms
```

4. **Bulkhead** — limit concurrent calls so one slow service doesn't exhaust parent's threads
```yaml
resilience4j:
  bulkhead:
    instances:
      order-service:
        maxConcurrentCalls: 10
```

5. **Log + alert** — log the `CallNotPermittedException` (thrown when OPEN), push to monitoring (Grafana/PagerDuty)
```java
public String fallback(Long id, CallNotPermittedException e) {
    log.error("Circuit OPEN for order-service, id={}", id);
    // trigger alert
    return "Service temporarily down";
}
```

> Rule: parent should **degrade gracefully** — respond with partial data, not fail entirely. Circuit breaker protects the parent from wasting threads on a dead downstream.

---

## Config Server Setup

**Server** (`config-server`)
```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/org/config-repo
```

**Client** (each microservice)
```yaml
# bootstrap.yml
spring:
  application:
    name: order-service
  config:
    import: optional:configserver:http://localhost:8888
```

- Config file in repo: `order-service.yml` or `order-service-prod.yml`

---

## ExecutorService — 100 Tasks, 10 Threads

```java
ExecutorService executor = Executors.newFixedThreadPool(10);

for (int i = 0; i < 100; i++) {
    int taskId = i;
    executor.submit(() -> {
        System.out.println("Task " + taskId + " by " + Thread.currentThread().getName());
    });
}

executor.shutdown();
executor.awaitTermination(1, TimeUnit.MINUTES);
```

- `newFixedThreadPool(10)` — max 10 threads, remaining tasks queue up

---

## Load Balancer in Microservice (Spring Cloud)

```yaml
# application.yml — client side
spring:
  cloud:
    loadbalancer:
      ribbon:
        enabled: false   # use Spring Cloud LoadBalancer, not Ribbon
```

```java
// RestTemplate with @LoadBalanced — resolves service name via Eureka
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
    return new RestTemplate();
}

// Usage
restTemplate.getForObject("http://ORDER-SERVICE/orders/1", String.class);
```

- `lb://SERVICE-NAME` in Gateway config does the same automatically

---

## Kafka — Producer & Consumer

**Producer**
```java
@Autowired
private KafkaTemplate<String, String> kafkaTemplate;

public void send(String message) {
    kafkaTemplate.send("order-topic", message);
}
```

**Consumer**
```java
@KafkaListener(topics = "order-topic", groupId = "order-group")
public void consume(String message) {
    System.out.println("Received: " + message);
}
```

**Config**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: order-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
```

**Key concepts**
- Topic — category/channel
- Partition — parallelism within a topic
- Group ID — consumers in same group share partitions (each message processed once)
- Offset — position of consumer in partition