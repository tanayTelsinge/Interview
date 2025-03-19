What tx does?
BEGIN TRANSACTION
 //business logic
 DEBIT FROM A
 CREDIT TO B
 COMMIT
 IF EXCEPTION
  ROLLBACK
END OF TRANSACTION

# Spring Boot `@Transactional` Annotation - Part 2

## 1. Declarative vs. Programmatic Transactions

### 1.1 Declarative Transactions (`@Transactional`)
Declarative transaction management uses the `@Transactional` annotation at the service layer. It is managed by Spring and does not require manual handling.

#### Example: Declarative Transaction
```java
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Transactional
    public void placeOrder(Order order) {
        orderRepository.save(order);
        // Any exception here will cause rollback
        if (order.getAmount() > 10000) {
            throw new RuntimeException("Order amount too high!");
        }
    }
}
```
**Key Points:**
- Spring manages the transaction automatically.
- If an exception occurs, the transaction is rolled back.
- Works well with checked exceptions unless `rollbackFor` is specified.

---

### 1.2 Programmatic Transactions (`TransactionTemplate`)
Programmatic transaction management gives explicit control over transactions using `TransactionTemplate` or `PlatformTransactionManager`.

#### Example: Programmatic Transaction
```java
@Service
public class OrderService {
    
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    public void placeOrder(Order order) {
        transactionTemplate.execute(status -> {
            orderRepository.save(order);
            if (order.getAmount() > 10000) {
                throw new RuntimeException("Order amount too high!");
            }
            return null;
        });
    }
}
```
**Key Points:**
- Explicitly defines transactional boundaries using `TransactionTemplate`.
- Provides better flexibility for managing transactions dynamically.
- Useful when finer control over transaction flow is needed.

---

## 2. Transaction Propagation Types
Spring offers different transaction propagation types to control how transactions behave when a method is called inside another transactional method.

| Propagation Type     | Behavior |
|----------------------|----------|
| **REQUIRED** *(default)* | Uses existing transaction or creates a new one if none exists. |
| **REQUIRES_NEW** | Suspends existing transaction and creates a new one. |
| **NESTED** | Runs within a nested transaction if an existing one exists. |
| **MANDATORY** | Requires an existing transaction; throws an exception if none exists. |
| **NEVER** | Must not run within a transaction; throws an exception if one exists. |
| **NOT_SUPPORTED** | Runs without a transaction, suspending any existing one. |
| **SUPPORTS** | Uses a transaction if one exists, otherwise runs without it. |

### Examples of Propagation Types

#### **REQUIRED (Default)**
```java
@Transactional(propagation = Propagation.REQUIRED)
public void methodA() {
    methodB(); // Runs in the same transaction
}

@Transactional(propagation = Propagation.REQUIRED)
public void methodB() {
    // Runs within the transaction of methodA
}
```

#### **REQUIRES_NEW**
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void methodA() {
    methodB(); // Runs in a separate transaction
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void methodB() {
    // Runs in a new transaction, independent of methodA
}
```

#### **NESTED**
```java
@Transactional(propagation = Propagation.NESTED)
public void methodA() {
    methodB(); // Runs in a nested transaction
}

@Transactional(propagation = Propagation.NESTED)
public void methodB() {
    // Can be rolled back independently of methodA
}
```

---

## Summary
- **Declarative (`@Transactional`)**: Simple and recommended for most cases.
- **Programmatic (`TransactionTemplate`)**: Provides finer control over transactions.
- **Propagation Types**: Controls how transactions interact when calling other transactional methods.



