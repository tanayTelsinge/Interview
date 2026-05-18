---
# DB

## How do you improve performance of a query?
- Indexes on WHERE, JOIN, ORDER BY, GROUP BY columns
- Avoid `SELECT *` — fetch only needed columns
- Use `EXPLAIN` / `EXPLAIN ANALYZE` — spot full scans, missing indexes
- Replace subqueries with JOINs
- Avoid functions on indexed columns in WHERE (`YEAR(col)` kills index)
- LIMIT/OFFSET or keyset pagination for large sets
- Redis / query-level caching
- Partitioning / sharding — reduce scan size
- Connection pooling — reduce connection overhead
- Normalize to cut redundancy; denormalize hot-read tables

---

## DROP vs TRUNCATE

| | DROP | TRUNCATE |
|---|---|---|
| Removes | Table + data | Data only |
| Rollback | No | Yes (Postgres), No (MySQL) |
| Speed | Slower | Faster |
| Triggers | No | No |
| Auto-increment | N/A | Resets |

---

## Temporary Table vs View

| | Temp Table | View |
|---|---|---|
| Storage | Physical | Virtual (no storage) |
| Scope | Session/transaction | Persistent |
| Indexes | Yes | No (except materialized) |
| Use case | Complex intermediate results | Abstraction / security |

- Materialized View = stores results physically, refreshed periodically

**Temp Table**
```sql
CREATE TEMPORARY TABLE temp_orders AS
SELECT user_id, SUM(amount) AS total FROM orders GROUP BY user_id;

SELECT * FROM temp_orders WHERE total > 1000;
```

**View**
```sql
CREATE VIEW active_users AS
SELECT id, name FROM users WHERE status = 'active';

SELECT * FROM active_users;
```

**Materialized View** (PostgreSQL)
```sql
CREATE MATERIALIZED VIEW monthly_sales AS
SELECT EXTRACT(MONTH FROM order_date) AS month, SUM(amount)
FROM orders GROUP BY month;

REFRESH MATERIALIZED VIEW monthly_sales;
```

---

## Indexing

- Data structure (B-Tree) for fast row lookup — O(log n) vs O(n) full scan

**Types**
- B-Tree — default, range + equality
- Hash — O(1) exact match, no range
- Bitmap — low-cardinality columns (data warehouse)
- Full-Text — text search
- Composite — multi-column, leftmost prefix rule
- Partial — index on subset of rows
- Clustered — data rows physically sorted by index (one per table)
- Non-Clustered — separate structure, pointer to row

**Cons**
- Slows INSERT/UPDATE/DELETE
- Extra storage
- Too many → query planner confusion
- Low-cardinality columns → ineffective

---

## Sharding

- Horizontal split of data across multiple DB instances
- Shard key → router directs query to correct shard

**Types**
- Range-based — IDs 1-1M shard1, 1M-2M shard2 (hotspot risk)
- Hash-based — uniform distribution, range queries harder
- Directory-based — lookup table maps key→shard (lookup is bottleneck)
- Geo-based — by region/country (data locality, compliance)

**Cons**
- Cross-shard queries expensive
- Re-sharding/rebalancing is hard
- No cross-shard transactions
- Joins across shards not straightforward

---

## DB Partitioning

- Split large table into partitions **within the same DB instance** (≠ sharding)

**Types**
- Horizontal — rows split (most common, e.g. orders by year)
- Range — by value range
- List — by discrete values
- Hash — uniform distribution
- Composite — combination

- Benefit: partition elimination (query pruning), easier archival
- Sharding = across machines | Partitioning = within one machine

---

## Stored Procedures — Pros/Cons

**Pros**
- Pre-compiled execution plan → faster
- Logic on DB server → less network traffic
- Grant execute without exposing table structure
- Reusable centralized logic
- Can wrap multiple statements in a transaction

**Cons**
- Hard to version control
- Debugging is painful
- Tight DB coupling — hard to migrate DB engine
- DB becomes bottleneck under load
- Unit testing is painful

---

## Clustered vs Non-Clustered Index

| | Clustered | Non-Clustered |
|---|---|---|
| Data | Physically sorted by key | Separate structure + pointer |
| Count | Only one per table | Multiple allowed |
| Range queries | Faster | Slower (pointer hops) |
| Example | PK in MySQL InnoDB | Any additional index |

- MySQL InnoDB: PK is always clustered index; non-clustered stores PK as pointer

---
---

# Java

## Spring Cloud — Key Components
- **Eureka** — service registry, discovery
- **API Gateway** — single entry, routing, auth, rate limiting
- **Config Server** — centralized config (Git-backed)
- **Feign Client** — declarative REST for inter-service calls
- **Resilience4j** — circuit breaker, retry, rate limiter
- **Zipkin + Sleuth** — distributed tracing
- **Spring Cloud Bus** — broadcast config changes via message broker

---

## JWT — Extract Token

```java
String bearerToken = request.getHeader("Authorization");
if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
    return bearerToken.substring(7);
}
```

---

## JWT — Validate Token

```java
Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
```

**What gets checked**
- Signature — re-signs header+payload, throws `SignatureException` if tampered
- Expiry — reads `exp`, throws `ExpiredJwtException` automatically
- Claims — manually verify `iss` (issuer), `aud` (audience)
- Revocation — store `jti` in Redis with TTL = token's remaining lifetime; check blacklist on each request

**In microservices**
- Validation at API Gateway only
- Gateway forwards identity via `X-User-Id`, `X-Roles` headers
- Downstream services trust gateway, skip re-validation
- Zero-trust: each service also validates with its own `aud`

---

## Security at Each Microservice Level

1. **AuthZ (Role check)** — `@PreAuthorize("hasRole('ADMIN')")` on endpoints; gateway does AuthN, service does AuthZ
2. **Trust boundary** — only accept from gateway/internal; enforce via network policy or `X-Internal-Token` header
3. **Service-to-service auth** — mTLS or OAuth2 client credentials (not user JWT)
4. **Input validation** — validate all payloads; never trust upstream blindly
5. **Rate limiting** — at gateway; critical services add Resilience4j too
6. **Secrets management** — Vault or env secrets; never in code

---

## Optimistic vs Pessimistic Locking

### Optimistic Locking
- Assumes conflicts are **rare** — no lock acquired on read
- Uses a `@Version` field (integer or timestamp); on update, checks version hasn't changed
- If version mismatch → throws `OptimisticLockException` → retry or surface error
- **Best for**: high-read, low-conflict scenarios (e.g. user profile updates)

```java
@Entity
public class Product {
    @Id Long id;
    int stock;

    @Version
    int version;  // JPA auto-increments on every UPDATE
}
```

### Pessimistic Locking
- Assumes conflicts are **likely** — acquires a DB-level lock on read
- Other transactions blocked until lock is released
- `PESSIMISTIC_WRITE` → `SELECT ... FOR UPDATE`; `PESSIMISTIC_READ` → shared lock
- **Best for**: high-contention scenarios (e.g. inventory deduction, seat booking)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Product findByIdForUpdate(@Param("id") Long id);
```

| | Optimistic | Pessimistic |
|---|---|---|
| Lock held | No | Yes (DB lock) |
| Conflict | Detected at commit | Prevented at read |
| Throughput | Higher | Lower |
| Risk | Rollback on conflict | Deadlock |
| Use case | Low contention | High contention |

### When to use which

**Use Optimistic when:**
- Reads far outnumber writes (social profiles, product catalog, config data)
- Conflicts are genuinely rare — retrying on failure is acceptable
- You want maximum throughput and can't afford DB-level locks
- Distributed systems / microservices where holding a DB lock across network calls is dangerous

**Use Pessimistic when:**
- Two users can realistically race on the same row (seat booking, flash sale inventory, bank debit)
- The cost of a failed transaction is high (money movement, ticket allocation)
- Operation is short — lock is held briefly, deadlock risk stays low
- You need guaranteed consistency over throughput

**Real interview example:**
- *E-commerce checkout* — pessimistic on `stock` column; you can't let two buyers both read `stock=1` and both commit
- *User settings update* — optimistic; two concurrent updates are rare; losing one is acceptable and retryable

---

## @Transactional — Propagation & Isolation

### Propagation — what happens when a transactional method calls another

| Type | Behavior |
|---|---|
| `REQUIRED` (default) | Join existing tx; create new if none |
| `REQUIRES_NEW` | Always start new tx; suspend existing |
| `NESTED` | Savepoint inside existing tx; inner rollback doesn't kill outer |
| `MANDATORY` | Must have existing tx; else exception |
| `NEVER` | Must NOT have tx; else exception |
| `SUPPORTS` | Join if exists; run without tx if none |
| `NOT_SUPPORTED` | Suspend existing tx; run without tx |

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void auditLog(String event) { ... }  // always its own tx
```

### Isolation — what concurrent transactions can see from each other

| Level | Dirty Read | Non-Repeatable Read | Phantom Read |
|---|---|---|---|
| `READ_UNCOMMITTED` | Yes | Yes | Yes |
| `READ_COMMITTED` (default PG) | No | Yes | Yes |
| `REPEATABLE_READ` (default MySQL) | No | No | Yes |
| `SERIALIZABLE` | No | No | No |

- **Dirty Read** — read uncommitted data from another tx
- **Non-Repeatable Read** — same row read twice gives different values (another tx updated it)
- **Phantom Read** — same query returns different row count (another tx inserted/deleted)

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void processOrder(Long id) { ... }
```

**Common interview answer**: default is `DEFAULT` (inherits DB default). Use `SERIALIZABLE` only when absolute correctness is needed — it kills throughput. Most apps live at `READ_COMMITTED`.
