# Caching — Interview Cheat Sheet

---

## What is Caching?

Storing frequently accessed data in fast memory so repeated requests don't hit
the database or external service. Reduces latency and DB load.

```
Without cache:  Request → DB (10-100ms) → Response
With cache:     Request → Cache (< 1ms) → Response
                         (miss) → DB → Cache → Response
```

---

## Types of Cache

| Type | Where | Scope | Examples |
|------|-------|-------|----------|
| In-process / Local | JVM heap | Single instance | Caffeine, Guava |
| Distributed | External server | All instances share | Redis, Memcached |
| CDN cache | Edge network | Static assets | Cloudflare, Cloud CDN |
| DB query cache | DB layer | DB server | Postgres, MySQL cache |

---

## Spring Boot Cache Abstraction

Spring provides `@EnableCaching` + annotations that work on top of any cache
provider (Caffeine, Redis, EhCache). You switch providers without changing code.

### Gradle Dependencies

```groovy
// Core cache abstraction — always needed
implementation 'org.springframework.boot:spring-boot-starter-cache'

// ── Local cache: Caffeine ──────────────────────────────────────────────────
implementation 'com.github.ben-manes.caffeine:caffeine:3.1.8'

// ── Distributed cache: Redis ───────────────────────────────────────────────
implementation 'org.springframework.boot:spring-boot-starter-data-redis'

// Optional: Redis + Spring Session (distributed sessions)
implementation 'org.springframework.session:spring-session-data-redis'
```

### Enable Caching
```java
@SpringBootApplication
@EnableCaching               // required — activates cache proxy
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## Core Annotations

### @Cacheable — cache the return value
```java
@Cacheable(value = "reservations", key = "#id")
public Reservation getById(Long id) {
    return reservationRepository.findById(id).orElseThrow();
    // First call: hits DB, stores result in cache under key "reservations::1"
    // Subsequent calls: returns from cache, method body never executes
}
```

### @CachePut — always execute method, update cache
```java
@CachePut(value = "reservations", key = "#reservation.id")
public Reservation update(Reservation reservation) {
    return reservationRepository.save(reservation);
    // Always runs. Saves to DB AND updates the cache entry.
    // Use after update so cache doesn't serve stale data.
}
```

### @CacheEvict — remove from cache
```java
@CacheEvict(value = "reservations", key = "#id")
public void delete(Long id) {
    reservationRepository.deleteById(id);
    // Removes the entry from cache after deletion.
}

// Evict all entries in a cache
@CacheEvict(value = "reservations", allEntries = true)
public void clearAll() { }
```

### @Caching — combine multiple cache operations
```java
@Caching(evict = {
    @CacheEvict(value = "reservations", key = "#reservation.id"),
    @CacheEvict(value = "availableRooms", allEntries = true)
})
public void cancelReservation(Reservation reservation) {
    reservation.setStatus("CANCELLED");
    reservationRepository.save(reservation);
}
```

### Conditional caching
```java
// Only cache if result is not null
@Cacheable(value = "guests", key = "#id", unless = "#result == null")
public Guest findGuest(Long id) { ... }

// Only cache for premium guests
@Cacheable(value = "guests", key = "#id", condition = "#id > 100")
public Guest findGuest(Long id) { ... }
```

---

## Local Cache — Caffeine

In-process, stored in JVM heap. Fast (nanoseconds). Lost on restart.
Best for: single-instance or read-heavy data that fits in memory.

### application.yml
```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m
    cache-names:
      - reservations
      - guests
      - roomAvailability
```

### Programmatic config (per-cache TTL)
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Different TTL per cache
        manager.registerCustomCache("reservations",
            Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats()          // enable hit/miss metrics
                .build());

        manager.registerCustomCache("roomAvailability",
            Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(Duration.ofMinutes(1))  // availability changes fast
                .build());

        manager.registerCustomCache("guests",
            Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofHours(1))
                .build());

        return manager;
    }
}
```

### Caffeine eviction policies

| Policy | Description |
|--------|-------------|
| `maximumSize` | Evict LRU entries when size exceeded |
| `maximumWeight` | Evict based on custom weight per entry |
| `expireAfterWrite` | Evict N time after entry was written |
| `expireAfterAccess` | Evict N time after last read or write |
| `refreshAfterWrite` | Reload in background after write TTL |

---

## Distributed Cache — Redis

External cache server shared across all service instances.
Best for: multiple pods on GKE, session data, rate limiting, pub/sub.

```
Pod 1 ─┐
Pod 2 ─┤── Redis Cluster ── same data for all pods
Pod 3 ─┘
```

### application.yml
```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost           # or Redis Cloud / Memorystore host
      port: 6379
      password: ${REDIS_PASSWORD}
      timeout: 2000ms
      lettuce:                  # Lettuce is default client (thread-safe)
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 2
```

### Redis CacheManager config
```java
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {

        // Default config — applied to all caches unless overridden
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        // Per-cache TTL overrides
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        cacheConfigs.put("reservations",
            defaultConfig.entryTtl(Duration.ofMinutes(10)));

        cacheConfigs.put("roomAvailability",
            defaultConfig.entryTtl(Duration.ofMinutes(1)));

        cacheConfigs.put("guests",
            defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
```

### RedisTemplate — direct Redis access (non-annotation)
```java
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    public boolean isAllowed(String userId) {
        String key = "rate:limit:" + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        return count <= 100;   // max 100 requests per minute
    }
}
```

---

## GCP — Cloud Memorystore (Managed Redis)

In a GCP/GKE setup you don't self-host Redis — you use Memorystore.

```yaml
spring:
  data:
    redis:
      host: 10.128.0.10       # Memorystore private IP (VPC internal)
      port: 6379
      # No password needed — secured via VPC + IAM
```

```
GKE Pod → VPC internal network → Cloud Memorystore (Redis)
                                  (no public internet exposure)
```

Memorystore handles replication, failover, and patching automatically.

---

## Cache Aside Pattern (most common)

Application manages cache manually — most flexible.

```
Read:
  1. Check cache
  2. If hit → return cached value
  3. If miss → query DB → store in cache → return

Write:
  1. Write to DB
  2. Invalidate (evict) cache entry
  3. Next read will repopulate from DB
```

```java
// Cache Aside — manual implementation
public Reservation getReservation(Long id) {
    String key = "reservation:" + id;
    Reservation cached = (Reservation) redisTemplate.opsForValue().get(key);
    if (cached != null) return cached;

    Reservation reservation = reservationRepository.findById(id).orElseThrow();
    redisTemplate.opsForValue().set(key, reservation, Duration.ofMinutes(10));
    return reservation;
}
```

---

## Cache Patterns Compared

| Pattern | How | Consistency | Use case |
|---------|-----|-------------|----------|
| Cache Aside | App manages read/write to cache | Eventual | Most common — read-heavy |
| Write Through | Write to DB and cache simultaneously | Strong | Write + read heavy |
| Write Behind | Write to cache first, async to DB | Eventual | Very write-heavy, some loss risk |
| Read Through | Cache fetches from DB on miss automatically | Eventual | Simplified app logic |

---

## Cache Stampede / Thundering Herd

Problem: Cache entry expires → 100 concurrent requests all miss → 100 DB hits.

Solutions:
1. **Locking** — first thread acquires lock, fetches from DB, others wait
2. **Probabilistic early expiry** — randomly refresh before TTL expires
3. **Caffeine refreshAfterWrite** — background refresh before entry expires

```java
// Redis lock to prevent stampede
public Reservation getWithLock(Long id) {
    String key = "reservation:" + id;
    String lockKey = "lock:reservation:" + id;

    Reservation cached = (Reservation) redisTemplate.opsForValue().get(key);
    if (cached != null) return cached;

    Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));

    if (Boolean.TRUE.equals(locked)) {
        Reservation reservation = reservationRepository.findById(id).orElseThrow();
        redisTemplate.opsForValue().set(key, reservation, Duration.ofMinutes(10));
        redisTemplate.delete(lockKey);
        return reservation;
    } else {
        // Another thread is fetching — brief wait and retry
        Thread.sleep(50);
        return getWithLock(id);
    }
}
```

---

## Cache Invalidation Strategies

1. **TTL (Time To Live)** — simplest, stale for TTL duration, no explicit eviction
2. **Event-driven eviction** — service publishes event, cache evicts on event
3. **Write-through** — update cache on every write, always fresh
4. **Manual evict** — `@CacheEvict` on update/delete methods

```
TTL only        → simple, stale data acceptable
Event-driven    → near real-time, more complex
Write-through   → always fresh, doubles write cost
```

---

## Local vs Distributed — When to Use

| Situation | Use |
|-----------|-----|
| Single instance / simple app | Caffeine (local) |
| Multiple pods on GKE | Redis (distributed) |
| Session data shared across pods | Redis |
| Rate limiting across pods | Redis |
| Reference data (country codes, config) | Caffeine (rarely changes) |
| User-specific data, short TTL | Redis |
| Response caching, heavy computation | Both — L1 Caffeine + L2 Redis |

### Two-level cache (L1 + L2)
```
Request → Caffeine (L1, in-pod, ~0ms)
           miss → Redis (L2, shared, ~1ms)
                   miss → DB (10-100ms)
```

```java
@Cacheable(value = "reservations", key = "#id", cacheManager = "caffeineCacheManager")
public Reservation getFromLocalCache(Long id) {
    return getFromRedis(id);   // L2 fallback
}

@Cacheable(value = "reservations", key = "#id", cacheManager = "redisCacheManager")
public Reservation getFromRedis(Long id) {
    return reservationRepository.findById(id).orElseThrow();   // L3 fallback
}
```

---

## Distributed Session with Redis

Problem: User logs in to Pod 1. Next request hits Pod 2. Session lost.
Solution: Store session in Redis — all pods share sessions.

```groovy
// Gradle
implementation 'org.springframework.session:spring-session-data-redis'
```

```java
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
@Configuration
public class SessionConfig { }
```

```yaml
spring:
  session:
    store-type: redis
    timeout: 30m
```

Now `HttpSession` is transparently stored in Redis — no code changes in controllers.

---

## Common Interview Questions

### Q: Caffeine vs Redis — which to use in microservices on GKE?

> "Caffeine is in-process — each pod has its own cache. If Pod 1 caches a
> reservation and Pod 2 receives the update request, Pod 2 evicts its copy but
> Pod 1's copy stays stale. For a multi-pod GKE deployment, Redis is the right
> choice — one shared cache across all pods, evictions are globally consistent.
> Caffeine still makes sense for truly static data like config values or country
> codes that never change at runtime."

---

### Q: What is cache stampede and how do you prevent it?

> "Cache stampede happens when a popular entry expires and many concurrent requests
> all miss the cache simultaneously, hammering the DB. Prevention: use a distributed
> lock in Redis — first thread acquires the lock, fetches from DB, repopulates cache,
> releases lock. Other threads wait and then hit the now-warm cache. Alternatively,
> Caffeine's refreshAfterWrite refreshes entries in the background before they expire,
> so the cache is never cold."

---

### Q: How do you handle cache invalidation in a microservices setup?

> "We use event-driven invalidation — when a reservation is updated, the service
> publishes an event to Pub/Sub. All service instances subscribe and evict the
> relevant cache key. This keeps caches consistent across pods without tight
> coupling. The tradeoff is a brief window of inconsistency between publish and
> consume, which is acceptable for read-heavy data. For writes that require
> immediate consistency we skip the cache entirely and use @CacheEvict."

---

### Q: What serializer do you use for Redis and why?

> "GenericJackson2JsonRedisSerializer — stores values as human-readable JSON,
> compatible with any class without requiring Serializable. The alternative,
> JdkSerializationRedisSerializer, uses Java serialization — brittle across
> versions and not human-readable. JSON is also cross-language, so non-Java
> services can read the same Redis keys if needed."

---

## Key Numbers

| | Value |
|--|-------|
| Caffeine lookup | ~10 nanoseconds |
| Redis lookup (local) | ~0.1ms |
| Redis lookup (network) | 0.5–2ms |
| DB query (simple) | 5–50ms |
| Recommended max TTL for availability data | 1–5 minutes |
| Recommended max TTL for user/session data | 30 minutes |
