# EPAM Systems — SSE Fullstack Java + Angular + React
## Interview Preparation Notes

---

## Table of Contents
1. [Core Java](#core-java)
2. [Java 8 Features](#java-8-features)
3. [Multithreading & Concurrency](#multithreading--concurrency)
4. [Collections](#collections)
5. [Spring Boot](#spring-boot)
6. [Hibernate & JPA](#hibernate--jpa)
7. [SQL](#sql)
8. [Kafka & Messaging](#kafka--messaging)
9. [REST API](#rest-api)
10. [Microservices & System Design](#microservices--system-design)
11. [DSA Problems](#dsa-problems)
12. [Frontend — JavaScript / Angular / React](#frontend--javascript--angular--react)
13. [AWS](#aws)
14. [SOLID Principles](#solid-principles)
15. [ACID & DB Isolation Levels](#acid--db-isolation-levels)
16. [Design Patterns](#design-patterns)

---

## Core Java

### OOP Concepts

#### Clean Code Principles
1. **Extract Method** — A function should do one thing well. Keep it 20–30 lines. Avoid deep nesting via early returns or helper functions.
2. **Minimize Parameters** — At most 3 params. Use objects for more. Reduces complexity and improves readability.
3. **Dependency Inversion** — Depend on abstractions, not concrete classes. Enables loose coupling and swappable implementations.

---

#### Abstract Class vs Interface
| Feature | Abstract Class | Interface |
|---|---|---|
| Abstraction | Partial (some concrete methods) | Full (no concrete methods*) |
| Multiple Inheritance | ❌ | ✅ |
| Constructor | ✅ | ❌ |
| Fields | Instance fields allowed | Only `static final` |

> *Java 8+ allows `default` methods in interfaces.

---

#### Multiple Inheritance with Interfaces — Diamond Problem

```java
interface A { void print(); }
interface B { void print(); }

class MyClass implements A, B {
    public void print() {
        System.out.println("Printing from MyClass");
    }
}
```

- Same signature → only **one implementation** needed.
- If both have `default` implementations → **must override**, else compile error.
- To call a specific interface's default method:

```java
class MyClass implements A, B {
    public void print() {
        A.super.print(); // Call A's default method
    }
}
```

> ⚠️ If return types differ with same method name → **compile error** (not overloading across interfaces).

---

### Serialization

**Serializable** — Converts object → byte stream (for file storage, network transfer, DB).  
**Deserialization** — Converts byte stream → object.

**Marker Interface** — No methods. Used as a tagging mechanism.
- JVM checks `instanceof Serializable` at runtime.
- If not implemented → throws `NotSerializableException`.

```java
public final void writeObject(Object obj) throws IOException {
    if (!(obj instanceof Serializable)) {
        throw new NotSerializableException(obj.getClass().getName());
    }
    writeNonNullObject(obj);
}
```

#### Serializable vs Externalizable

| Feature | Serializable | Externalizable |
|---|---|---|
| Control | JVM handles automatically | Developer implements manually |
| Performance | Slower (all fields) | Faster (only required fields) |
| Methods | None | `writeExternal()`, `readExternal()` |
| Customization | Limited (`transient`, `writeObject`) | Fully customizable |
| Security | Risk of unintended exposure | Full control |
| Use When | Need automatic serialization | Complex objects, performance issues, sensitive fields |

```java
class Person implements Externalizable {
    String name;
    int age;

    public Person() {} // Mandatory default constructor

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name);
        out.writeInt(age);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        name = in.readUTF();
        age = in.readInt();
    }
}
```

---

### Covariant Return Type
Introduced in Java 5. Overriding method can return a **subtype** of the parent's return type. No explicit casting needed.

```java
class Animal {
    Animal getAnimal() { return new Animal(); }
}

class Dog extends Animal {
    @Override
    Dog getAnimal() { return new Dog(); } // Covariant return ✅
}

Dog dog = new Dog().getAnimal(); // No cast needed ✅
```

---

### Immutable Class
```java
public final class ImmutableClass {
    private final String name;
    private final int age;

    public ImmutableClass(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
}
```
Rules: `final` class, `private final` fields, no setters, deep copy for mutable fields.

---

## Java 8 Features

### Functional Interfaces

| Interface | Input | Output | Use Case |
|---|---|---|---|
| `Function<T, R>` | T | R | Transformations |
| `Predicate<T>` | T | boolean | Filtering |
| `Consumer<T>` | T | void | Actions (logging, printing) |
| `Supplier<R>` | None | R | Providing/generating values |
| `BiPredicate<T, U>` | T, U | boolean | Two-arg filtering |

```java
Function<Integer, String>   intToStr  = num -> "Number: " + num;
Predicate<Integer>          isEven    = n -> n % 2 == 0;
Consumer<String>            printer   = msg -> System.out.println(msg);
Supplier<Double>            random    = () -> Math.random();
BiPredicate<Integer, Integer> sumEven = (a, b) -> (a + b) % 2 == 0;
```

> `@FunctionalInterface` annotation is optional but recommended.

---

### Stream API

#### Find second non-repeating character
```java
String s = "stress"; // Expected: 'r'

Character result = s.chars()
    .mapToObj(c -> (char) c)
    .collect(Collectors.groupingBy(
        Function.identity(),
        LinkedHashMap::new,
        Collectors.counting()
    ))
    .entrySet().stream()
    .filter(e -> e.getValue() == 1)
    .skip(1)
    .findFirst()
    .get()
    .getKey();
```

#### Most frequent element in array
```java
Map<Integer, Long> freqMap = Arrays.stream(arr)
    .boxed()
    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
```

#### Average marks per student
```java
Map<String, Double> avgMarks = students.stream()
    .collect(Collectors.toMap(
        StudentsGrade::getStudentName,
        s -> s.getStudentGrades().values().stream()
              .mapToInt(Integer::intValue)
              .average()
              .orElse(0.0)
    ));
```

#### Unique words starting with `@`
```java
String[] str = {"This is @autowired", "@qualifier annotation"};

Set<String> uniqueWords = Arrays.stream(str)
    .flatMap(sentence -> Arrays.stream(sentence.split(" ")))
    .filter(word -> word.startsWith("@"))
    .collect(Collectors.toSet());
// Output: [@autowired, @qualifier]
```

#### Cube filter and average
```java
// Find cube of numbers, filter total > 100, find average
OptionalDouble avg = IntStream.rangeClosed(1, 10)
    .map(n -> n * n * n)
    .filter(n -> n > 100)
    .average();
```

#### Sort employees — Noida, reverse alphabetical
```java
employees.stream()
    .filter(e -> e.getCity().equals("Noida"))
    .sorted(Comparator.comparing(Employee::getName).reversed())
    .collect(Collectors.toList());
```

---

## Multithreading & Concurrency

### `volatile`
- **Guarantees visibility** — writes go directly to RAM, reads from RAM (no CPU cache).
- Does **not** guarantee atomicity.
- Use for simple flags, not counters.

```java
private volatile boolean running = true;
```

#### Without `volatile` — The Problem (Stale Cache Read)

```
Thread 2 writes Flag=F → updates its own Cache only (not RAM yet)
Thread 1 reads Flag   → reads from its Cache or RAM → gets stale Flag=T ❌

Thread 1  ←── CPU ←── Cache(T) ←────────── RAM (Flag=T)
                                                ↑
Thread 2  ←── CPU ←── Cache(F)  [Write]  not flushed yet
```

> Thread 1 sees `Flag=T` (stale) even though Thread 2 already set it to `F`.

#### With `volatile` — The Fix (Direct RAM Access)

```
Thread 2 writes Flag=F → goes DIRECTLY to RAM
Thread 1 reads Flag   → reads DIRECTLY from RAM → gets Flag=F ✅

Thread 1  ←── CPU ←── Cache ←──── RAM (Flag=F) ← Volatile Shared Variable
                                       ↑
Thread 2  ──────────────── Write ──────┘  (bypasses cache)
```

> Both threads always read/write directly to RAM. No stale values.

---

### `synchronized` vs `ReentrantLock`

| Feature | `synchronized` | `ReentrantLock` |
|---|---|---|
| Explicit unlock | ❌ | ✅ (must call `unlock()`) |
| Try lock | ❌ | ✅ `tryLock()` |
| Fairness | ❌ | ✅ `new ReentrantLock(true)` |
| Interruptible | ❌ | ✅ |

---

### `ThreadLocal`
Each thread gets its own isolated copy of a variable.

```java
ThreadLocal<User> currentUser = new ThreadLocal<>();
currentUser.set(user);
currentUser.get();
currentUser.remove(); // ⚠️ Always remove in thread pools!
```

> **Memory leak risk** — thread pools reuse threads. Stale `ThreadLocal` values leak to next request if not removed.

---

### ExecutorService
```java
// ⚠️ Unbounded queue — risky in production
ExecutorService pool = Executors.newFixedThreadPool(10);

// ✅ Production-safe — bounded queue + backpressure
ExecutorService pool = new ThreadPoolExecutor(
    coreSize, maxSize, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(500),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

#### Runnable vs Callable

| Feature | `Runnable` | `Callable<T>` |
|---|---|---|
| Return type | `void` | `T` |
| Checked exceptions | ❌ | ✅ |
| Used with | `execute()` | `submit()` |

---

### Future vs CompletableFuture

| Feature | `Future` | `CompletableFuture` |
|---|---|---|
| Result retrieval | `get()` — blocking | `thenApply()` — non-blocking |
| Exception handling | `try-catch` | `exceptionally()`, `handle()` |
| Chaining | ❌ | ✅ |
| Multiple tasks | `invokeAll()` | `allOf()`, `anyOf()` |

```java
CompletableFuture
    .supplyAsync(() -> fetchUser(id))
    .thenApplyAsync(user -> enrichUser(user))
    .thenAccept(user -> save(user))
    .exceptionally(ex -> { log(ex); return null; });
```

> `thenApply` = map | `thenCompose` = flatMap (returns CF itself)

---

### Virtual Threads (Java 21)
```java
ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor();
```
- Millions of virtual threads vs thousands of platform threads.
- Ideal for **I/O-bound** workloads.
- ⚠️ Avoid `synchronized` on I/O — pins carrier thread. Use `ReentrantLock` instead.

---

### Deadlock vs Race Condition

| | Deadlock | Race Condition |
|---|---|---|
| Threads progress? | ❌ Stuck forever | ✅ Complete, but wrong result |
| Problem | Circular lock waiting | Unsynchronized shared state |
| Fix | Lock ordering, `tryLock` | Atomics, synchronization |

**Deadlock fix — always lock in same order:**
```java
if (accountA.id < accountB.id) {
    lock(accountA); lock(accountB);
} else {
    lock(accountB); lock(accountA);
}
```

---

## Collections

### HashSet Internal Working
- Backed by `HashMap` (elements = keys, dummy object = values).
- Uses `hashCode()` for bucket placement.
- Uses `equals()` to check duplicates within same bucket.
- Java 8+: Bucket switches from LinkedList → Red-Black Tree at 8+ collisions.
- `O(1)` average, `O(n)` worst case.

### ArrayList vs LinkedList

| | `ArrayList` | `LinkedList` |
|---|---|---|
| Access | O(1) | O(n) |
| Insert/Delete (middle) | O(n) | O(1) |
| Memory | Less (array) | More (node pointers) |
| Use when | Read-heavy | Insert/delete-heavy |

### TreeSet vs HashSet

| | `HashSet` | `TreeSet` |
|---|---|---|
| Order | Unordered | Sorted (natural/Comparator) |
| Performance | O(1) | O(log n) |
| Null | Allows 1 null | ❌ No null |

---

## Spring Boot

### Bean Lifecycle
```
Constructor → @PostConstruct / initMethod → [In Use] → @PreDestroy / destroyMethod
```

```java
@Component
public class MyBean {
    public MyBean() { System.out.println("1. Constructor"); }

    @PostConstruct
    public void init() { System.out.println("2. @PostConstruct"); }

    @PreDestroy
    public void destroy() { System.out.println("3. @PreDestroy"); }
}
```

---

### Prototype Bean in Singleton — Problem & Fix

```java
@Scope("prototype")
class AbcService { ... }

@Component
class XyzService {
    @Autowired
    AbcService abcService; // ⚠️ Injected once — always same instance!
}
```

**Fix — use `ApplicationContext` or `@Lookup`:**
```java
@Autowired
ApplicationContext context;

public void doWork() {
    AbcService abc = context.getBean(AbcService.class); // New instance each time ✅
}
```

---

### Conditional Bean Creation
```java
@Bean
@ConditionalOnProperty(name = "feature.enabled", havingValue = "true")
public MyService myService() { return new MyService(); }
```

---

### Constructor vs Setter Injection

| | Constructor | Setter |
|---|---|---|
| Mandatory deps | ✅ Enforced | ❌ Optional |
| Immutability | ✅ | ❌ |
| Error detection | Compile time | Runtime |
| Circular deps | Hard to create | Possible |

> **Constructor injection is preferred** — errors caught at compile time.

---

### `@Transactional` — Internal Working
Spring uses **AOP proxy**. When `@Transactional` is detected:
1. Proxy intercepts the method call.
2. Opens a DB transaction before the method.
3. Commits on success, rolls back on `RuntimeException`.
4. Self-invocation bypasses proxy → `@Transactional` has no effect on same-class calls.

---

### Key Spring Annotations

| Annotation | Purpose |
|---|---|
| `@Component` | Generic bean |
| `@Service` | Service layer |
| `@Repository` | DAO layer |
| `@RestController` | `@Controller` + `@ResponseBody` |
| `@Autowired` | Dependency injection |
| `@Qualifier` | Specify bean when multiple exist |
| `@Primary` | Default bean |
| `@Scope` | Bean scope |
| `@PostConstruct` | Init callback |
| `@PreDestroy` | Destroy callback |
| `@Transactional` | Transaction management |
| `@ConditionalOnProperty` | Conditional bean |
| `@Value` | Inject property values |

---

### Global Exception Handling
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                             .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(new ErrorResponse("Something went wrong"));
    }
}
```

---

## Hibernate & JPA

### Mappings

```java
// OneToOne
@OneToOne(targetEntity = Department.class)
private Department department;

// ManyToOne
@ManyToOne(cascade = CascadeType.ALL)
@JoinColumn(name = "address_id")
private Address address;

// ManyToMany
@ManyToMany
@JoinTable(
    name = "DegreeStudentThirdTable",
    joinColumns = @JoinColumn(name = "StudentId"),
    inverseJoinColumns = @JoinColumn(name = "CertificateId")
)
private List<Degree> degrees;
```

---

### Cache Types

| Level | Scope | Default | Implementation |
|---|---|---|---|
| 1st Level (Session) | Per Session | ✅ Enabled | Built-in |
| 2nd Level (SessionFactory) | Across sessions | ❌ Disabled | EHCache, Infinispan |
| Query Cache | Query result sets | ❌ Disabled | Requires 2nd level |

```java
// Enable query cache
query.setCacheable(true);
```

---

### EntityManager & PersistenceContext

```java
@Repository
public class UserRepository {

    @PersistenceContext
    private EntityManager entityManager; // Injected per-transaction

    @Transactional
    public void saveUser(User user) {
        entityManager.persist(user); // Tracked automatically
    }
}
```

> `@PersistenceContext` = JPA's equivalent of Hibernate's `Session`. Manages entity lifecycle and auto-flushes on transaction commit.

---

## SQL

### Employees with salary > average (grouped by entry_date)
```sql
SELECT e.*
FROM employees e
JOIN (
    SELECT entry_date, AVG(salary) AS avg_salary
    FROM employees
    GROUP BY entry_date
) AS avg_table ON avg_table.entry_date = e.entry_date
WHERE e.salary > avg_table.avg_salary;
```

---

## Kafka & Messaging

### How Kafka Works
- **Producers** publish messages to **topics**.
- **Brokers** store messages in **partitions** (replicated).
- **Consumers** pull messages from partitions via consumer groups.
- Each consumer in a group reads from exclusive partitions.
- Messages retained for a fixed duration (even after consumption).
- Managed by **ZooKeeper** (pre-3.0) or **KRaft** (3.0+).

### Kafka vs RabbitMQ

| | RabbitMQ | Kafka |
|---|---|---|
| Type | Message Broker | Event Streaming Platform |
| Model | Push-based | Pull-based |
| Message retention | Deleted after consumption | Retained for TTL |
| Throughput | Moderate | Very high |
| Use case | Microservice messaging, task queues | Log processing, analytics, event sourcing |

---

## REST API

### API Versioning Strategies

| Strategy | Example | Best For |
|---|---|---|
| URL Path | `/api/v1/employees` | Public APIs |
| Query Param | `/api/employees?version=1` | Simple cases |
| Header-based | `X-API-Version: 1` | Enterprise APIs |
| Subdomain | `v1.api.company.com` | Large-scale |
| API Gateway | Route by URL/header | Microservices |

---

### Richardson Maturity Model (RMM)

| Level | Description |
|---|---|
| 0 | Single endpoint (only POST) |
| 1 | Resource-based endpoints |
| 2 | Proper HTTP methods + status codes |
| 3 | HATEOAS — responses include hypermedia links |

```json
{
  "id": 123,
  "name": "Alice",
  "_links": {
    "self": { "href": "/employees/123" },
    "update": { "href": "/employees/123", "method": "PUT" },
    "delete": { "href": "/employees/123", "method": "DELETE" }
  }
}
```

---

## Microservices & System Design

### SAGA Pattern
Manages **distributed transactions** across microservices.

- **Choreography** — Each service publishes events; others react. No central coordinator.
- **Orchestration** — A central orchestrator (e.g., Camunda) directs each service step.

Each step has a **compensating transaction** to roll back on failure.

---

### Circuit Breaker (Resilience4j)
```
CLOSED → (failure threshold exceeded) → OPEN → (timeout) → HALF_OPEN → (success) → CLOSED
```

---

### Payment Service — Strategy + Factory Pattern
```java
interface PaymentProvider {
    void pay(double amount);
}

class GPay implements PaymentProvider { ... }
class PhonePe implements PaymentProvider { ... }
class AmazonPay implements PaymentProvider { ... }

class PaymentFactory {
    public static PaymentProvider getProvider(String type) {
        return switch (type) {
            case "GPAY"     -> new GPay();
            case "PHONEPE"  -> new PhonePe();
            case "AMAZONPAY"-> new AmazonPay();
            default -> throw new IllegalArgumentException("Unknown provider: " + type);
        };
    }
}
```

---

### How Microservices Communicate
- **Synchronous** — REST (HTTP), gRPC
- **Asynchronous** — Kafka, RabbitMQ, SQS
- **Service Discovery** — Eureka
- **API Gateway** — Kong, AWS API Gateway, Spring Cloud Gateway

---

### Monitoring Multiple Microservices
- **Distributed tracing** — Zipkin, Jaeger (trace requests across services)
- **Metrics** — Prometheus + Grafana
- **Logging** — ELK Stack (Elasticsearch, Logstash, Kibana)
- **Health checks** — Spring Actuator + `/health` endpoints
- **Alerting** — PagerDuty, Grafana alerts

---

## DSA Problems

### Product of Array Except Self
```java
// Input: [1,2,3,4,5] → Output: [120,60,40,30,24]
int[] result = new int[arr.length];
int prefix = 1;
for (int i = 0; i < arr.length; i++) {
    result[i] = prefix;
    prefix *= arr[i];
}
int suffix = 1;
for (int i = arr.length - 1; i >= 0; i--) {
    result[i] *= suffix;
    suffix *= arr[i];
}
```

### Group Anagrams
```java
Map<String, List<String>> groupAnagrams(String[] strs) {
    return Arrays.stream(strs)
        .collect(Collectors.groupingBy(s -> {
            char[] ch = s.toCharArray();
            Arrays.sort(ch);
            return new String(ch);
        }));
}
```

### Middle of Linked List (Fast/Slow Pointer)
```java
public ListNode middleNode(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }
    return slow;
}
```

### Missing Numbers in Range
```java
// Input: [4,2,9] → Output: 3 5 6 7 8
int min = Arrays.stream(arr).min().getAsInt();
int max = Arrays.stream(arr).max().getAsInt();
Set<Integer> set = Arrays.stream(arr).boxed().collect(Collectors.toSet());
IntStream.rangeClosed(min, max)
    .filter(n -> !set.contains(n))
    .forEach(n -> System.out.print(n + " "));
```

### Max Sum Subarray (Kadane's Algorithm)
```java
int maxSum(int[] arr) {
    int max = arr[0], current = arr[0];
    for (int i = 1; i < arr.length; i++) {
        current = Math.max(arr[i], current + arr[i]);
        max = Math.max(max, current);
    }
    return max;
}
```

### Merge K Sorted Linked Lists
```java
// Use PriorityQueue (min-heap)
PriorityQueue<ListNode> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.val));
for (ListNode node : lists) if (node != null) pq.offer(node);
ListNode dummy = new ListNode(0), curr = dummy;
while (!pq.isEmpty()) {
    curr.next = pq.poll();
    curr = curr.next;
    if (curr.next != null) pq.offer(curr.next);
}
return dummy.next;
```

---

## Frontend — JavaScript / Angular / React

### JavaScript Key Concepts

| Concept | Summary |
|---|---|
| `var` vs `let` | `var` = function-scoped, hoisted; `let` = block-scoped |
| `null` vs `undefined` | `null` = intentional absence; `undefined` = not assigned |
| Hoisting | `var` and function declarations hoisted; `let`/`const` not |
| Closure | Function retaining access to outer scope after outer fn returns |
| Prototype | Objects inherit from `prototype` chain |
| `call/apply/bind` | Explicitly set `this` context |

### Promises
```javascript
fetch('/api/data')
  .then(res => res.json())
  .then(data => console.log(data))
  .catch(err => console.error(err));

// Promise methods
Promise.all([p1, p2])       // Wait for all
Promise.race([p1, p2])      // First to resolve
Promise.allSettled([p1, p2]) // All results, success or failure
```

### Flatten Array (without built-in)
```javascript
function flatten(arr) {
    return arr.reduce((acc, val) =>
        Array.isArray(val) ? acc.concat(flatten(val)) : acc.concat(val), []);
}
// Input: [1,2,[3],[[4]],[[[5]]]] → Output: [1,2,3,4,5]
```

### Sort by Age then Salary
```javascript
arr.sort((a, b) => a.age !== b.age ? a.age - b.age : a.salary - b.salary);
```

---

### Angular Key Concepts

| Concept | Summary |
|---|---|
| `@ViewChild` | Access single child component/element |
| `@ViewChildren` | Access multiple child components |
| `BehaviorSubject` | RxJS Subject with initial value; emits last value to new subscribers |
| `Observable` | Lazy; only emits when subscribed |
| `ngZone` | Controls Angular's change detection zone |
| Pipes | Transform template output (e.g., `date`, `currency`, custom) |

---

### React Key Concepts

| Hook | Purpose |
|---|---|
| `useEffect` | Runs after paint; async side effects |
| `useLayoutEffect` | Runs before paint; sync DOM operations |
| `useRef` | Persist value across renders without re-render |
| `useCallback` | Memoize function reference |
| `useMemo` | Memoize computed value |

**Reconciliation** — React's diffing algorithm using virtual DOM + `key` prop to minimize actual DOM updates.

---

## AWS

### DynamoDB
- **Partition Key** — Determines the partition where data is stored (like hash key). Must be unique if used alone.
- **Sort Key** — Used alongside partition key to sort/query items within same partition.
- Together = **composite primary key**.

### SQS — Purge Queue
```
AWS Console → SQS → Select Queue → Actions → Purge
```
Or via CLI:
```bash
aws sqs purge-queue --queue-url https://sqs.region.amazonaws.com/account/queue-name
```

---

## SOLID Principles

| Principle | Summary | Example |
|---|---|---|
| **S** — SRP | One reason to change | Separate `UserService` and `AuthService` |
| **O** — OCP | Open for extension, closed for modification | `Payment` interface → `CreditCardPayment`, `PayPalPayment` |
| **L** — LSP | Subtypes replace base types without breaking behavior | `Penguin` shouldn't extend `Bird` if it can't fly |
| **I** — ISP | Don't force unused method implementations | `Printer` and `Scanner` interfaces separately |
| **D** — DIP | Depend on abstractions, not concretions | Inject `NotificationService` interface, not `EmailService` |

---

## ACID & DB Isolation Levels

### ACID
- **Atomicity** — All or nothing. Debit + credit both succeed or both roll back.
- **Consistency** — DB moves from one valid state to another. Constraints enforced.
- **Isolation** — Concurrent transactions don't interfere.
- **Durability** — Committed data survives crashes (written to disk/logs).

### Isolation Levels

| Level | Dirty Read | Non-Repeatable Read | Phantom Read |
|---|---|---|---|
| Read Uncommitted | ✅ Possible | ✅ Possible | ✅ Possible |
| Read Committed | ❌ | ✅ Possible | ✅ Possible |
| Repeatable Read | ❌ | ❌ | ✅ Possible |
| Serializable | ❌ | ❌ | ❌ |

> **Serializable** = safest but slowest. Locks entire table during transaction.

---

## Design Patterns

### Strategy + Factory (Payment Example)
> See [Payment Service](#payment-service--strategy--factory-pattern) above.

### Builder Pattern
```java
User user = User.builder()
    .name("Tanay")
    .age(28)
    .city("Pune")
    .build();
```

### Singleton
```java
public class Singleton {
    private static volatile Singleton instance;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) instance = new Singleton();
            }
        }
        return instance;
    }
}
```

### Abstract Factory vs Factory
| | Factory Method | Abstract Factory |
|---|---|---|
| Creates | One product | Family of related products |
| Complexity | Simple | More abstract |
| Use when | One type varies | Multiple related types vary together |

### Observer Pattern
Publisher emits events → Subscribers react. Used in Kafka, Spring Events, RxJS Observables.

---

## JWT

**Three parts** (Base64 encoded, dot-separated):
1. **Header** — Algorithm + token type
2. **Payload** — Claims (subject, expiry, roles)
3. **Signature** — HMAC of header + payload using secret key

```
xxxxx.yyyyy.zzzzz
```

Verification: Re-compute signature and compare. Tampered payload → signature mismatch → rejected.

---

*Last updated: 2025*
