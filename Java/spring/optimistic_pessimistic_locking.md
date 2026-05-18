# Optimistic vs Pessimistic Locking in JPA/Hibernate

## Core Idea

| | Optimistic | Pessimistic |
|---|---|---|
| Assumption | Conflicts are rare | Conflicts are frequent |
| Mechanism | Version check at commit time | DB-level lock acquired upfront |
| Failure mode | `OptimisticLockException` on conflict | Blocked/waiting (or timeout) |
| Performance | Better throughput (no DB locks) | Lower throughput, avoids retries |
| DB impact | No lock held during transaction | Lock held for full transaction duration |

---

## Optimistic Locking

Uses a `@Version` column (integer or timestamp). JPA checks the version hasn't changed before committing.

### Setup

```java
@Entity
public class Product {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private int stock;

    @Version
    private int version;  // JPA manages this automatically
}
```

### What JPA does under the hood

```sql
-- Read
SELECT id, name, stock, version FROM product WHERE id = 1;
-- version = 5

-- On commit, JPA adds a version check:
UPDATE product SET stock = 9, version = 6
WHERE id = 1 AND version = 5;
-- If 0 rows updated → another transaction changed it → OptimisticLockException
```

### Handling the exception (with retry)

```java
@Service
public class ProductService {

    @Autowired
    private ProductRepository repo;

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3)
    @Transactional
    public void decrementStock(Long productId) {
        Product product = repo.findById(productId).orElseThrow();
        if (product.getStock() < 1) throw new IllegalStateException("Out of stock");
        product.setStock(product.getStock() - 1);
        // version check happens automatically on flush/commit
    }
}
```

### LockModeType.OPTIMISTIC vs OPTIMISTIC_FORCE_INCREMENT

```java
// OPTIMISTIC (default with @Version)
// Version incremented only if entity was modified
Product p = em.find(Product.class, 1L, LockModeType.OPTIMISTIC);

// OPTIMISTIC_FORCE_INCREMENT
// Version incremented even if entity was NOT modified
// Use case: you read a parent entity and modified a child —
// force the parent version to bump so concurrent readers see a change
Product p = em.find(Product.class, 1L, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
```

---

## Pessimistic Locking

Acquires a real DB lock (SELECT FOR UPDATE or SELECT FOR SHARE) at read time.
No version column needed.

### LockModeType options

| Mode | SQL issued | Behaviour |
|------|-----------|-----------|
| `PESSIMISTIC_READ` | `SELECT ... FOR SHARE` | Others can read, blocked on write |
| `PESSIMISTIC_WRITE` | `SELECT ... FOR UPDATE` | Others blocked on both read and write |
| `PESSIMISTIC_FORCE_INCREMENT` | `SELECT ... FOR UPDATE` + version bump | Like WRITE but also increments `@Version` |

### Repository with pessimistic lock

```java
public interface AccountRepository extends JpaRepository<Account, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);
}
```

### Transfer example (classic high-contention scenario)

```java
@Service
public class BankingService {

    @Autowired
    private AccountRepository accountRepo;

    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        // Always lock in consistent order to prevent deadlock
        Long firstId  = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);

        Account first  = accountRepo.findByIdWithLock(firstId).orElseThrow();
        Account second = accountRepo.findByIdWithLock(secondId).orElseThrow();

        Account from = first.getId().equals(fromId) ? first : second;
        Account to   = first.getId().equals(toId)   ? first : second;

        if (from.getBalance().compareTo(amount) < 0)
            throw new IllegalStateException("Insufficient funds");

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        // Lock released on transaction commit
    }
}
```

### Setting a lock timeout (avoid indefinite waiting)

```java
// In repository method
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")) // ms
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM Account a WHERE a.id = :id")
Optional<Account> findByIdWithLock(@Param("id") Long id);
```

```yaml
# Or globally in application.yml (Hibernate)
spring:
  jpa:
    properties:
      jakarta.persistence.lock.timeout: 3000
```

---

## When to Use What?

### Contention = degree of competition between transactions for the same resource

### Low Contention → Optimistic Locking

Conflicts are rare, reads dominate.

| Scenario | Why optimistic works |
|----------|---------------------|
| E-commerce product catalog | Thousands browse, very few update prices/stock |
| User profile updates | Users rarely edit their own profile concurrently |
| CMS article editing | One author edits at a time; detect conflict if two editors clash |

### High Contention → Pessimistic Locking

Multiple transactions frequently update the same rows.

| Scenario | Why pessimistic works |
|----------|-----------------------|
| Bank account transfers | Concurrent debits must be serialized |
| Seat/hotel room booking | Last-seat race condition must not double-book |
| Real-time stock trading | Buy/sell orders for the same stock |
| Inventory flash sale | Thousands hit "buy" simultaneously |

---

## Common Pitfalls

### 1. Optimistic lock retry storm
If many threads conflict, retries cause a thundering herd.
Fix: add exponential backoff, or switch to pessimistic for hot rows.

### 2. Pessimistic deadlock
Two transactions lock rows in opposite order → deadlock.
Fix: always acquire locks in the same canonical order (e.g. lower ID first).

### 3. Forgetting `@Transactional` with pessimistic lock
The `SELECT FOR UPDATE` lock is released immediately if there's no active transaction.

```java
// WRONG — lock released before you use the entity
Account a = accountRepo.findByIdWithLock(id).orElseThrow();

// RIGHT — keep inside @Transactional method
@Transactional
public void process(Long id) {
    Account a = accountRepo.findByIdWithLock(id).orElseThrow();
    // ... modify ...
}
```

### 4. Version field exposed in DTO
Expose the version in your API response and echo it back on updates — clients need it for correct optimistic lock behavior.

```java
public record UpdateProductRequest(String name, int stock, int version) {}
```

---

## Quick Decision Guide

```
Is the data read >> written?
  YES → Optimistic (@Version)
  NO  →
    Can you tolerate retries?
      YES → Optimistic with @Retryable
      NO  → Pessimistic (PESSIMISTIC_WRITE)
            + lock timeout
            + canonical lock ordering to prevent deadlock
```
