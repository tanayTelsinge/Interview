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

![image](https://github.com/user-attachments/assets/0ece1c4b-8b77-45af-aad7-37ddae7a0d08)
##### Approach 1 : take object of Tx Manager (second img), use that object to perform transaction.
![image](https://github.com/user-attachments/assets/544cc18c-5287-46db-a168-f7749225d2b8)

![image](https://github.com/user-attachments/assets/793928ea-b097-4adc-ae23-519c397c86f4)
##### Approach 2 :use transaction Template
![image](https://github.com/user-attachments/assets/20922548-4f24-4cb8-9ae3-18c0ad9d31fb)

![image](https://github.com/user-attachments/assets/71314cb4-2c9c-43a6-a3aa-7cc70c21ee54)
- Transaction template ( a blueprint) has execute method, it basically has template (rollback, commit), execute method accepts Tx callback (business logic)>
  ![image](https://github.com/user-attachments/assets/ea777758-59f2-4c76-8f11-b7d34c3d1eba)
  ![image](https://github.com/user-attachments/assets/2cd56b55-2a5a-4b7f-8412-2b9c47ed60e5)




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

- Note : how to check what transaction is currently running 
- use TransactionSynchronizationManager. --> methods 
![image](https://github.com/user-attachments/assets/0911cd0a-b1ab-4aaa-9f37-16cb920883e3)

### Examples of Propagation Types
- Note : propogation logic is present in AbstractPLatformTransactionManager.java
- ![image](https://github.com/user-attachments/assets/c8775fb0-f721-4499-b4d5-0c5e6ee324b7)

#### **REQUIRED (Default)**
![image](https://github.com/user-attachments/assets/d1c93897-0bde-4048-aca5-22f8d8613914)

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

suspends means pause.
(Issue - Hi, I think the issue you have with the mutation-service is due to the Propagation.REQUIRES_NEW transactionals which have been added in a couple places.
You might be holding more connections than you expect, resulting in a dead lock when it comes to connections (as each connection will wait for the other to release).
![image](https://github.com/user-attachments/assets/68b0de2d-e414-464a-916e-546868c05e30)

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

### **SUPPORTS**
- If parent t present, use it, else execute w/o tx
![image](https://github.com/user-attachments/assets/c0351293-0bcd-49ad-a9bf-efbf4ae63402)

### **NOT_SUPPORTED**
- If parent tx present, suspends it and execute w/o tx, resume.
- else method w/o tx.
![image](https://github.com/user-attachments/assets/82c5ab3b-54a0-4946-a52d-c6b032cf0003)

### **MANDATORY**
- Parent tx should be present.
![image](https://github.com/user-attachments/assets/025a2570-2de4-4c7a-a152-7fa996b7eb14)

### **NEVER**
![image](https://github.com/user-attachments/assets/bec54f59-79d7-496e-9ffb-43b40fd514fe)


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

---

### Isolation Level
- It tells how changes made by one tx are visible to other tx in parallel. (imp for concurrency in LLD too).
- 4 types of isolation levels - READ_UNCOMMITTED, READ_COMMITTED, REPEATABLE_COMMITTED, SERIALIZABLE.
- Default isolation depends on DB, most relational once use READ_COMMITTED.
- ![image](https://github.com/user-attachments/assets/7e01bd46-ca00-4bf6-aedf-40b1c3551cb2)
- ![image](https://github.com/user-attachments/assets/052c2048-2aa6-4336-8154-26ccf2e28580)
- Concurrency depends on this isolation levels.
- ![image](https://github.com/user-attachments/assets/aac3dc6d-f232-45b4-9917-c8144571049a)

- Que? High Concurrency is good right?
- No. Understand when to use which.
- 3 Problems need to understand which isolation lvl resolves.
1. Dirty read problem
 ![image](https://github.com/user-attachments/assets/8ed52322-6461-4c47-b715-497268548ddf)

example:
- At time T2, Tx B updates row to status 'booked', but not committed.
- **But at time T3, Tx A reads row with status 'booked' hence the dirty read.
- At time T4, tx B some issue occurs and rollback, but problem is Tx A has read uncommitted changes which is not in DB. ****DIRTY READ****
![image](https://github.com/user-attachments/assets/2c063b7d-eaf5-46b0-a1cf-4d6973bce6ed)

2. Non-Repeatable Read Problem

   ![image](https://github.com/user-attachments/assets/1cb55103-a7d3-4dc5-8eb2-743b68e46746)

   Example:
   - Tx A at Time T2, it read row status 'Free', but when it reads at T4, status 'booked', hence non repeatable read problem.
   ![image](https://github.com/user-attachments/assets/317b41f2-5be3-4456-a11d-21fa431caeef)

3. Phantom Read problem
![image](https://github.com/user-attachments/assets/276159e7-dda8-4db2-a058-647bb3832a56)
Example :
- For Tx A, at time T2, rows 2.
- But at Time T4, rows returned are 3 rows. (i.e.) different rows with same query.
![image](https://github.com/user-attachments/assets/c9a4a519-e171-4af7-a78f-106917476fbc)


#### DB Locking Types
- shared lock a.k.a. read lock. - suppose two tx, tx A reading DB row 1, then shared lock, if Tx B wants to read row 1, can it read?
- Yes, Tx B put shared lock and can read it. i.e CAN READ BUT CANNOT MODIFY.
- Exclusive lock a.k.a. write lock - if Tx A is modifying row 1, it puts exclusive lock, then Tx B CANNOT READ OR WRITE. (due to exclusive lock).
![image](https://github.com/user-attachments/assets/49f6fab6-6cbb-4cd4-8475-2c5163990e9e)

### Isolation levels and DB Locking Strategy
- so at READ_UNCCOMMITTED has no locks, hence high concurrency, but risky.
- READ COMMITTED has some locks - READ - shared lock and releases once it reads, WRITE - exclusive lock keeps it till the end of tx, it solves dirty read, but not other two.
![image](https://github.com/user-attachments/assets/afe85eee-5753-4a48-9250-f2321d46dd8b)










   





