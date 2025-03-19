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
Declarative transaction management uses the `@Transactional` annotation at the service layer.
It is managed by Spring and does not require manual handling. (Tx Manager will be selected by Spring boot).
If you want to specify which tx manager to select, 
create appconfig with @Configuration -> create bean of PlatFormTxManager and create TxManager, for eg. here its DataSourceTransactionManager

![image](https://github.com/user-attachments/assets/3c7b4065-5080-45f7-a8d6-3d0b1e900318)
![image](https://github.com/user-attachments/assets/c0617580-332e-4f4c-aca3-c0cc04fc155b)



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


## Transaction heirarchy
![image](https://github.com/user-attachments/assets/0485ffde-813d-4298-abcf-cc8f05624690)
![image](https://github.com/user-attachments/assets/02872a4a-1683-4c3f-bdeb-7f604f48410b)


- TransactionManager is empty interface.
- Platform TransactionManager has 3 abstract methods.
- Abstract Platform Tx Manager provides default implementations.
- Different Mgrs - logics of commit rollback is almost same, just specific things are overridden or changed.
- DataSource Tx Manager - JDBC Tx Manager --> we can write queries.
- Hibernate Tx Manager - entity and transaction related to it.
- JPA Tx Manager - inbuilt JPA methods for entities.
- JTA Tx Manager - used in distributed tx or 2 phase commit. (others for local tx).

- 


