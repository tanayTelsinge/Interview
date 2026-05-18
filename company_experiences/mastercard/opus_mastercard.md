# Mastercard Interview — Backend Heavy Round

---

## Intro
Keep it crisp: years of experience, current project (payment domain), tech stack (Java, Spring Boot, REST, DB), and one impactful contribution.

---

## Payment Input Flow (CVV, Card Number)
- Frontend collects card details via a **PCI-DSS compliant hosted field / iframe** (we never touch raw card data on our servers directly).
- Data is sent over **HTTPS/TLS** to the backend.
- On the backend, we immediately **tokenize** the card number via a vault (e.g., HashiCorp Vault or a payment processor like Stripe/Braintree). The real PAN is replaced with a token.
- CVV is **never stored** — it is only used at the time of authorization and discarded.
- The token is what travels through our internal services and gets stored in the DB.

Flow:
```
User → HTTPS → API Gateway → Payment Service → Tokenization Vault
                                             ↓
                                     Token stored in DB
                                     CVV used only for auth → discarded
```

---

## DB Structure (Payment)
```
users          → user_id, name, email (PII masked/encrypted)
accounts       → account_id, user_id, token (not real PAN), bank_code
transactions   → txn_id, account_id, amount, status, created_at
audit_log      → log_id, txn_id, action, actor, timestamp
```
- Sensitive columns (PAN token, email) are **encrypted at rest** using AES-256.
- DB connection is over TLS; access is role-restricted (principle of least privilege).

---

## Security — How We Handled It

**Anonymization:** Replace real values (PAN, email) with masked/pseudonymous values in logs and non-prod DBs. E.g., `**** **** **** 4242`.

**Encryption (interviewer's suggestion):** Encrypt PAN/sensitive fields using AES-256 before storing. Only the payment service holds the decryption key (stored in Vault, not in code).

**Spring Security implementation:**
- Used `SecurityFilterChain` (not deprecated `WebSecurityConfigurerAdapter`).
- JWT-based stateless authentication: custom `JwtAuthenticationFilter` plugged into the filter chain before `UsernamePasswordAuthenticationFilter`.
- Role-based access control via `@PreAuthorize("hasRole('PAYMENT_ADMIN')")`.
- Method-level security enabled via `@EnableMethodSecurity`.
- CSRF disabled for REST APIs (stateless); CORS configured explicitly.
- Password encoding via `BCryptPasswordEncoder`.
- OAuth2 resource server for inter-service communication.

```java
@Bean
SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/public/**").permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

---

## Scenarios

### 1. Email per environment (local / dev / prod)

Use **Spring Profiles** with profile-specific YAML files and a `MailService` interface with different implementations.

```
application.yml          → common config
application-local.yml    → mail.enabled=false
application-dev.yml      → mail.to=myemail@company.com
application-prod.yml     → mail.to=${CLIENT_EMAIL}
```

```java
@Profile("local")
@Service
class NoOpMailService implements MailService {
    public void send(String to, String body) { /* do nothing */ }
}

@Profile({"dev","prod"})
@Service
class SmtpMailService implements MailService {
    // reads mail.to from yml, sends real email
}
```

Run with: `--spring.profiles.active=dev`

---

### 2. Spring Stereotype Annotation Hierarchy

```
@Component                          ← root stereotype
    ├── @Service                    ← business logic layer
    ├── @Repository                 ← data access layer (+ exception translation)
    ├── @Controller                 ← Spring MVC, returns views
    │       └── @RestController     ← @Controller + @ResponseBody (returns JSON/XML)
    └── @Configuration              ← bean factory, CGLIB proxied
```

**All of the above are `@Component`** — Spring component scan picks them all up and registers them as beans. The specializations just add extra behaviour on top.

| Annotation | Extends | Extra Behaviour |
|---|---|---|
| `@Component` | — | Base. Just registers a bean. |
| `@Service` | `@Component` | Semantic label for service layer. No technical difference from `@Component`. |
| `@Repository` | `@Component` | Enables **persistence exception translation** — wraps DB exceptions into Spring's `DataAccessException`. |
| `@Controller` | `@Component` | Marks as MVC controller. Works with `ViewResolver` to return HTML views. |
| `@RestController` | `@Controller` | Adds `@ResponseBody` to every method — responses are serialized directly to JSON/XML, no view resolution. |
| `@Configuration` | `@Component` | CGLIB proxied. `@Bean` methods return **singletons** (calls are intercepted). Used to define bean factories. |

---

#### `@Configuration` is the superset of `@Component` — Why?

`@Configuration` does everything `@Component` does **plus** CGLIB proxying:

```java
@Configuration
class AppConfig {
    @Bean
    ServiceA serviceA() {
        return new ServiceA(dataSource()); // dataSource() call intercepted by CGLIB
    }

    @Bean
    DataSource dataSource() {
        return new HikariDataSource(); // only created ONCE, cached
    }
}
```

If this were `@Component` instead of `@Configuration`, `dataSource()` would be called as a plain Java method — creating a **new** `DataSource` every time. With `@Configuration`, CGLIB ensures the same instance is always returned.

---

#### Quick Mental Model

```
Need a general bean?              → @Component
Business logic / service?         → @Service
DB queries / DAO?                 → @Repository
REST API endpoint?                → @RestController
Define/wire multiple beans?       → @Configuration
```

---

### `@Configuration` vs `@Component` — which is superset?

`@Configuration` **is a superset** of `@Component`.

| Feature | `@Component` | `@Configuration` |
|---|---|---|
| Registers as Spring bean | Yes | Yes |
| `@Bean` methods produce singleton beans | No (each call = new instance) | Yes (CGLIB proxied — returns same instance) |
| Used for | General beans | Bean factory / config classes |

`@Configuration` classes are proxied by CGLIB so that `@Bean` method calls are intercepted and return the same singleton. `@Component` classes are not proxied — calling a `@Bean` method inside a `@Component` class creates a new object each time.

```java
@Configuration
class AppConfig {
    @Bean
    ServiceA serviceA() { return new ServiceA(dataSource()); }

    @Bean
    DataSource dataSource() { return new DataSource(); } // called once, cached
}
```

---

### 3. Encryption vs Encoding vs Serialization

| | Purpose | Reversible? | Example |
|---|---|---|---|
| **Encoding** | Format transformation for transmission/storage | Yes (no key needed) | Base64, URL encoding |
| **Encryption** | Hide data from unauthorized parties | Yes (key needed) | AES-256, RSA |
| **Serialization** | Convert object to bytes/string for storage or transfer | Yes | Java serialization, JSON, Protobuf |

For payment data:
- **Encode** → Base64 to transmit binary data over HTTP.
- **Encrypt** → AES-256 to store PAN/sensitive fields in DB.
- **Serialize** → JSON/Protobuf when sending payment event to Kafka.

---

### 4. Analytics API — 10K users at 9 AM, data refreshes every 15 min

**Design:**

```
Client → API Gateway (rate limiting, auth)
              ↓
        Load Balancer
       /      |       \
  Instance1 Instance2 Instance3   (horizontal scaling)
              ↓
         Redis Cache  ← TTL = 15 min (matches data refresh rate)
              ↓ (cache miss)
         Analytics DB / Data Warehouse
```

- **Redis cache** with TTL of 15 minutes: at 9 AM all users hit cache, not DB.
- A **scheduler** (`@Scheduled(fixedRate = 900000)`) refreshes the cache every 15 min by pulling fresh data and writing to Redis.
- **API Gateway** for auth, rate limiting (throttle per user to prevent abuse).
- **3 instances** behind a load balancer for horizontal scaling.
- **Async pre-warm**: before 9 AM (e.g., 8:55 AM), a job pre-computes and loads analytics into Redis so the 9 AM spike hits warm cache.
- Response with `Cache-Control: max-age=900` so browser/CDN also caches.

```java
@Scheduled(fixedRate = 900_000)
public void refreshAnalyticsCache() {
    Analytics data = analyticsRepo.fetchLatest();
    redisTemplate.opsForValue().set("analytics:latest", data, 15, MINUTES);
}
```

---

### 5. `@RestController` Internal Working

1. Request comes in → **DispatcherServlet** intercepts it (front controller pattern).
2. `DispatcherServlet` consults **HandlerMapping** (e.g., `RequestMappingHandlerMapping`) to find which controller method matches the URL + HTTP method.
3. **HandlerAdapter** (`RequestMappingHandlerAdapter`) invokes the matched method.
4. Method parameters are resolved by **ArgumentResolvers** (e.g., `@RequestBody` → `HttpMessageConverter` deserializes JSON to Java object using Jackson).
5. Method executes, returns an object.
6. Since `@RestController = @Controller + @ResponseBody`, the return value is serialized back to JSON via `HttpMessageConverter` and written to the HTTP response directly (no view resolution).
7. Response is sent back to the client.

`@RestController` = `@Controller` + `@ResponseBody` on every method — skips `ViewResolver`.

---

### 6. Feature Worked End to End
*(Tailor to your actual project — example answer:)*

"I built the **payment reconciliation service** end to end. I designed the DB schema for storing transaction states, wrote the Spring Batch job to reconcile bank responses every night, exposed REST APIs for the ops team to query mismatches, added Spring Security JWT auth, wrote unit + integration tests, and deployed it via CI/CD pipeline. I also added monitoring alerts on reconciliation failure count."

---

### 7. Strategy Pattern — How I Implemented It

Used Strategy to handle different **payment gateway providers** (e.g., Stripe, Razorpay, PayPal) without if-else chains.

```java
// Strategy interface
interface PaymentGateway {
    PaymentResponse process(PaymentRequest request);
}

// Concrete strategies
@Component("stripe")
class StripeGateway implements PaymentGateway { ... }

@Component("razorpay")
class RazorpayGateway implements PaymentGateway { ... }

// Context — Spring injects all implementations into a map
@Service
class PaymentService {
    private final Map<String, PaymentGateway> gateways;

    PaymentService(Map<String, PaymentGateway> gateways) {
        this.gateways = gateways;
    }

    public PaymentResponse pay(String provider, PaymentRequest req) {
        return gateways.get(provider).process(req);
    }
}
```

Spring auto-populates the map using bean names as keys — zero if-else, fully open/closed principle compliant.

---

### 8. Throttling in Spring

Throttling = **rate limiting** — restricting how many requests a client can make in a given time window to protect services from abuse/overload.

**Ways to implement in Spring:**

1. **API Gateway level** (recommended): Kong, AWS API Gateway, or Spring Cloud Gateway with built-in rate limiter.

```yaml
# Spring Cloud Gateway with Redis rate limiter
filters:
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 10   # 10 req/sec
      redis-rate-limiter.burstCapacity: 20
```

2. **Bucket4j library** in Spring Boot:
```java
Bucket bucket = Bucket.builder()
    .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
    .build();

// In filter/interceptor:
if (!bucket.tryConsume(1)) {
    response.setStatus(429); // Too Many Requests
}
```

3. **`@RateLimiter` from Resilience4j** for method-level throttling.

---

### 9. What are the 5 Scopes in Spring?

Spring Bean Scopes define **how many instances** of a bean Spring creates and how long they live.

| Scope | Instances | Lifetime | Use Case |
|---|---|---|---|
| **singleton** | 1 per Spring container | Entire app lifetime | Stateless services, repositories (default) |
| **prototype** | New instance every time | Until GC (Spring doesn't manage destruction) | Stateful beans, command objects |
| **request** | 1 per HTTP request | Duration of one HTTP request | Web — request-specific data |
| **session** | 1 per HTTP session | Duration of user session | Web — user session state (e.g., cart) |
| **application** | 1 per ServletContext | Entire web app lifetime | Web — app-wide shared state |

> There is also **websocket** scope (1 per WebSocket session) sometimes mentioned as a 6th.

```java
@Bean
@Scope("prototype")
public PaymentCommand paymentCommand() { return new PaymentCommand(); }

// Or with annotation on the class:
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext { ... }
```

**Key gotcha:** Injecting a `prototype` bean into a `singleton` bean — the singleton gets only **one** prototype instance (created at startup). Fix: use `@Lookup`, `ObjectFactory<T>`, or `ApplicationContext.getBean()` to get a fresh instance each time.

---

### 10. What is PCI-DSS Compliance — What Do We Do Technically?

**PCI-DSS** (Payment Card Industry Data Security Standard) is a set of security standards that any system that **stores, processes, or transmits cardholder data** must follow. It is mandated by card networks (Visa, Mastercard, Amex).

Cardholder data includes:
- **PAN** — Primary Account Number (card number)
- **CVV/CVC** — Card verification value
- **Expiry date, cardholder name**

---

#### The 12 PCI-DSS Requirements (grouped technically)

**1. Network Security**
- Build and maintain a **firewall** to protect cardholder data.
- Never use vendor-supplied default passwords.

**2. Protect Cardholder Data**
- **Encrypt PAN at rest** using AES-256.
- **Never store CVV** after authorization — ever. Not in DB, not in logs.
- Mask PAN when displaying: `**** **** **** 4242`.
- Use **tokenization** — replace PAN with a token that has no exploitable value.

**3. Encrypt Data in Transit**
- Use **TLS 1.2+** for all data transmission. No HTTP, no older TLS versions.
- Encrypt API calls between internal services too (mTLS).

**4. Vulnerability Management**
- Keep all systems **patched** (OS, libraries, frameworks).
- Use **anti-virus/malware** protection.
- Run regular **dependency vulnerability scans** (e.g., OWASP Dependency-Check, Snyk).

**5. Access Control**
- **Least privilege** — each service/user gets only the access it needs.
- Role-based access control (RBAC) on all APIs and DB.
- Unique user IDs — no shared credentials.
- **MFA** for all admin/privileged access.

**6. Monitor and Test**
- **Audit logs** for every access to cardholder data — who, what, when.
- Logs must be tamper-proof and retained for at least 1 year.
- Run **penetration testing** at least annually.
- Use **intrusion detection systems (IDS)**.

**7. Information Security Policy**
- Maintain a formal security policy for all personnel.

---

#### What We Do Technically (Interview Answer)

```
1. Tokenization
   Real PAN → stored in secure vault (e.g., HashiCorp Vault)
   Token (random ID) → used in our DB and services
   Token has no mathematical relationship to real PAN

2. Encryption at Rest
   Sensitive DB columns encrypted with AES-256
   Encryption keys stored in Vault, rotated periodically
   Keys never hardcoded or stored in application config

3. Encryption in Transit
   All endpoints use TLS 1.2+
   HTTP endpoints blocked at API Gateway
   Inter-service communication uses mTLS

4. CVV Handling
   CVV accepted only at payment initiation
   Passed directly to payment processor
   Never logged, never stored — even temporarily

5. Data Masking
   PAN displayed as **** **** **** 4242 in UI and logs
   Logs scrubbed — no sensitive fields ever appear in log output

6. Audit Logging
   Every read/write to payment data is logged
   Log: actor, action, timestamp, resource ID
   Logs shipped to tamper-proof centralized store (e.g., Splunk, ELK)

7. Access Control
   DB users have only SELECT/INSERT on specific tables — no DROP/ALTER
   API endpoints secured with JWT + RBAC (@PreAuthorize)
   Admin APIs require MFA

8. Vulnerability Management
   OWASP Dependency-Check in CI pipeline — fails build on critical CVEs
   Regular pen tests and DAST scans (e.g., OWASP ZAP)
   Container images scanned before deployment
```

---

#### Key Distinction for Interviews

| Term | Meaning | PCI Relevance |
|---|---|---|
| **Tokenization** | Replace PAN with a non-sensitive token | Reduces PCI scope — token is useless if stolen |
| **Encryption** | Scramble data, reversible with key | Protects PAN at rest in DB |
| **Masking** | Show partial data (**** 4242) | For display/logs only |
| **Hashing** | One-way transform | For CVV verification (never store raw CVV) |

> **Scope reduction tip:** If you use a third-party tokenization vault (Stripe, Braintree), your servers never see the real PAN — this dramatically reduces PCI-DSS scope and audit burden.

---

### 11. How to Handle Processing of Large CSV — 10 Million Records

**Core principle: never load all 10M rows into memory at once. Stream, chunk, and process in parallel.**

---

#### Approach 1 — Spring Batch (Best for Interview Answer)

Spring Batch is purpose-built for this. It reads in **chunks**, processes, and writes — if it fails midway it can **restart from the last checkpoint**.

```
CSV File (10M rows)
      ↓
  FlatFileItemReader      ← reads line by line, never loads all into memory
      ↓ (chunk of 1000)
  ItemProcessor           ← transform / validate each record
      ↓
  ItemWriter              ← bulk insert to DB / send to Kafka
      ↓
  JobRepository           ← tracks progress, enables restart on failure
```

```java
@Bean
public Job csvImportJob(JobRepository jobRepository, Step step) {
    return new JobBuilder("csvImportJob", jobRepository)
        .start(step)
        .build();
}

@Bean
public Step step(JobRepository jobRepository, PlatformTransactionManager txManager) {
    return new StepBuilder("step", jobRepository)
        .<PaymentRecord, PaymentRecord>chunk(1000, txManager) // process 1000 at a time
        .reader(flatFileItemReader())
        .processor(paymentProcessor())
        .writer(jdbcBatchItemWriter())
        .build();
}

@Bean
public FlatFileItemReader<PaymentRecord> flatFileItemReader() {
    return new FlatFileItemReaderBuilder<PaymentRecord>()
        .name("paymentReader")
        .resource(new FileSystemResource("payments.csv"))
        .delimited()
        .names("accountId", "amount", "currency")
        .targetType(PaymentRecord.class)
        .build();
}
```

**Why chunk size 1000?** Balance between memory and DB round trips. Too small = too many DB calls. Too large = OOM risk.

---

#### Approach 2 — Parallel Processing (Scale Further)

For 10M rows, even chunked sequential processing may be slow. Add parallelism:

```
CSV File (10M rows)
      ↓
  Split into partitions   (Partitioner — e.g., by line range or file split)
      ↓
  ThreadPoolTaskExecutor  (e.g., 8 threads)
  /    |    |    \
Part1 Part2 Part3 Part4   ← each thread processes its chunk independently
      ↓
  Aggregated write to DB
```

```java
@Bean
public Step partitionedStep(JobRepository repo, Step workerStep) {
    return new StepBuilder("partitionedStep", repo)
        .partitioner("workerStep", new MultiResourcePartitioner())
        .step(workerStep)
        .taskExecutor(new SimpleAsyncTaskExecutor())
        .gridSize(8) // 8 parallel threads
        .build();
}
```

Or split the CSV into smaller files upfront:
```bash
split -l 1000000 payments.csv chunk_  # splits into 10 files of 1M rows each
```

---

#### Approach 3 — Streaming with BufferedReader (No Spring Batch)

If Spring Batch is overkill:

```java
try (BufferedReader reader = new BufferedReader(new FileReader("payments.csv"))) {
    List<PaymentRecord> batch = new ArrayList<>(1000);
    String line;
    while ((line = reader.readLine()) != null) {
        batch.add(parse(line));
        if (batch.size() == 1000) {
            bulkInsert(batch);  // DB batch insert
            batch.clear();
        }
    }
    if (!batch.isEmpty()) bulkInsert(batch); // flush remaining
}
```

---

#### Key Optimizations

| Problem | Solution |
|---|---|
| OOM on large file | Stream line by line — never `readAllLines()` |
| Slow DB inserts | JDBC batch insert (`addBatch()` / `executeBatch()`) or `COPY` command in Postgres |
| Single thread too slow | Partition file, use thread pool |
| Failure midway | Spring Batch restart from last committed chunk |
| Validation failures | Skip policy — skip bad records, log them, continue |
| File too large for one server | Upload to S3, trigger Lambda/worker per chunk |

---

#### Skip & Retry Policy (Fault Tolerance)

```java
.<PaymentRecord, PaymentRecord>chunk(1000, txManager)
    .reader(reader())
    .processor(processor())
    .writer(writer())
    .faultTolerant()
    .skip(ParseException.class)      // skip bad CSV rows
    .skipLimit(500)                  // allow max 500 skips
    .retry(TransientDataAccessException.class)  // retry on DB timeout
    .retryLimit(3)
    .build();
```

---

#### Interview Answer (Spoken Format)

> "For 10 million records I'd use **Spring Batch** with chunk-oriented processing — read 1000 rows at a time, process, bulk insert to DB, never load everything into memory. I'd add a **partitioner** to split the file and process chunks in parallel using a thread pool. For fault tolerance, I'd configure a skip policy for bad records and retry on transient DB errors. Spring Batch also tracks job state in its `JobRepository`, so if the job fails at record 5 million, it restarts from there — not from the beginning."

---

### 12. NFRs (Non-Functional Requirements) of Payment Architecture

| NFR | Requirement | How Achieved |
|---|---|---|
| **Availability** | 99.99% uptime | Multi-region deployment, active-active, health checks |
| **Latency** | < 300ms p99 | Redis cache, async processing, connection pooling |
| **Scalability** | Handle 10x traffic spikes | Horizontal scaling, Kubernetes HPA, load balancer |
| **Security** | PCI-DSS compliance | TLS, AES-256 encryption, tokenization, RBAC, audit logs |
| **Data Integrity** | No lost/duplicate transactions | Idempotency keys, DB transactions (ACID), Kafka exactly-once |
| **Observability** | Fast incident detection | Distributed tracing (Zipkin), metrics (Prometheus/Grafana), structured logs |
| **Disaster Recovery** | RPO < 1 min, RTO < 5 min | DB replication, automated failover, backup strategy |
| **Compliance** | PCI-DSS, GDPR | Data masking, audit trails, data retention policies |