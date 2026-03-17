# Design Patterns — Interview Masterclass

> **Interview Reality Check**: You won't be asked "explain the Strategy pattern."
> You'll get: "Design a payment system that supports UPI, cards, and wallets — and can add new methods without changing existing code."
> You're expected to arrive at the pattern naturally, THEN name it.

---

## Pattern Frequency in Senior Interviews (India)

| Tier | Patterns | Expected Depth |
|------|----------|----------------|
| **Tier 1** (must code cold) | Strategy, Observer, Factory, Singleton | Full implementation + thread-safety variants |
| **Tier 2** (practice well) | State, Builder, Decorator, Command, Composite | Implementation + when to use |
| **Tier 3** (conceptual only) | Chain of Responsibility, Adapter, Facade, Template Method, Proxy, Flyweight | Know the intent and one example |

---

## How Patterns Map to SOLID

| Pattern | SOLID Principle Enforced |
|---------|--------------------------|
| Strategy | OCP (extend without modifying), DIP (depend on abstractions) |
| Observer | OCP (add subscribers without changing publisher), SRP |
| Factory | DIP (client doesn't depend on concrete classes) |
| Decorator | OCP (add behavior without subclassing), SRP |
| State | SRP (each state class owns its own behavior), OCP |
| Builder | SRP (construction logic separate from business logic) |

**Interview tip**: When asked "how is this design SOLID?", map the pattern to the principle above.

---

## The Golden Rule for Interviews

> **Design first, name second.**
>
> Do NOT say: "I'll use the Strategy pattern here."
> DO say: "I'll extract the varying part (payment logic) into an interface, pass different implementations at runtime. This happens to be the Strategy pattern."
>
> The first shows textbook recitation. The second shows design thinking.

---

## Pattern 1 — Strategy

### Intent
Define a family of algorithms, encapsulate each one, make them interchangeable.
The context (caller) doesn't know or care which algorithm it's running.

### When to Use
- You have an `if/else` or `switch` on a type to decide behavior
- The behavior varies independently from the context that uses it
- You want to swap behavior at runtime

### Interview Scenario
> "Design a checkout system where users can pay via Credit Card, UPI, or Wallet. New payment methods will be added in future."

**Red flag answer**: `if (type.equals("CREDIT_CARD")) { ... } else if (type.equals("UPI")) { ... }`

**Signal answer**: Define `PaymentStrategy` interface, inject the concrete strategy.

### The Violation (Bad Pattern)
```java
// BAD: Every new payment method requires modifying this class
public void processPayment(String type, double amount) {
    if (type.equals("CREDIT_CARD")) {
        double fee = amount * 0.02;
        System.out.println("Charging card: " + (amount + fee));
    } else if (type.equals("UPI")) {
        System.out.println("UPI transfer: " + amount);
    } else if (type.equals("WALLET")) {
        System.out.println("Wallet debit: " + amount);
    }
    // Adding "NET_BANKING" requires opening and editing this class = OCP violation
}
```

### The Fix (Good Pattern)
```java
public interface PaymentStrategy {
    void pay(double amount);
    String getPaymentMethodName();
}

public class CheckoutService {
    private PaymentStrategy strategy;

    public CheckoutService(PaymentStrategy strategy) {  // DIP: constructor injection
        this.strategy = strategy;
    }

    public void checkout(double amount) {
        strategy.pay(amount);  // OCP: new methods don't change this class
    }
}
```

### Common Mistakes Senior Candidates Make
1. Forgetting to inject strategy via constructor (using `new` inside is DIP violation)
2. Storing mutable state in the strategy (strategies should be stateless when possible)
3. Over-applying: if you only have 2-3 fixed variants, a simple `if/else` is more readable

---

## Pattern 2 — Observer

### Intent
Define a one-to-many dependency so that when one object changes state,
all its dependents are notified and updated automatically.

### When to Use
- State changes in one object require updating many other objects
- You don't know (or don't want to hardcode) how many dependents there are
- Objects should be loosely coupled — publisher shouldn't know about subscribers

### Interview Scenario
> "Design a stock price notification system — multiple clients (email, SMS, push) subscribe to price changes."

### Thread-Safety Concern (Senior Differentiator)
The naive observer list is not thread-safe. Two issues:
1. **ConcurrentModificationException** if observer registers/unregisters while notification fires
2. **Notification order vs registration order** — usually irrelevant, but know it

**Fix**: Use `CopyOnWriteArrayList` for the observer list.

```java
// BAD: Not thread-safe
private List<Observer> observers = new ArrayList<>();

// GOOD: Thread-safe, good for read-heavy (notifications >> register/unregister)
private List<Observer> observers = new CopyOnWriteArrayList<>();
```

### Memory Leak Trap
> **Classic interview question**: "What's wrong with this Observer implementation?"
> **Answer**: If subscribers never call `unsubscribe()`, the publisher holds strong references,
> preventing garbage collection. This is the classic Observer memory leak.
>
> **Fix**: WeakReference to observers, or explicit lifecycle management (unsubscribe on destroy).

### Common Mistakes
1. Forgetting thread-safety (instant senior filter)
2. Not handling exceptions in one observer — a bad observer should NOT stop others from being notified (use try-catch in the notify loop)
3. Memory leaks (explained above)

---

## Pattern 3 — Factory (Factory Method + Simple Factory)

### Intent
Define an interface for creating an object, but let subclasses/implementations decide which class to instantiate.
Decouples the client from the concrete class it uses.

### Interview Scenario
> "Your notification service sends Email, SMS, and Push notifications. Add WhatsApp without touching the client code."

### Simple Factory vs Factory Method
- **Simple Factory** (not a GoF pattern, but common in interviews): One factory class with `create(type)` method
- **Factory Method** (GoF pattern): Abstract creator with `createProduct()` method, subclasses override

For most interviews, Simple Factory is sufficient. Know the distinction if asked.

```java
// Simple Factory
public class NotificationFactory {
    public static Notification create(String type) {
        return switch (type) {
            case "EMAIL" -> new EmailNotification();
            case "SMS"   -> new SMSNotification();
            default -> throw new IllegalArgumentException("Unknown: " + type);
        };
    }
}
```

**Note**: The `switch` here is fine — it's isolated to one place. Adding a new type only changes the factory, not the client.

### Factory vs Strategy
| | Factory | Strategy |
|---|---|---|
| **Purpose** | Object creation | Algorithm selection |
| **Question** | Which class to instantiate? | Which behavior to execute? |
| **Combined** | Use Factory to create the right Strategy | |

### Common Mistakes
1. Using factory when `new ConcreteClass()` is completely fine (over-engineering)
2. Factory knowing too much about the objects it creates (SRP violation)
3. Not using factory when the creation logic is genuinely complex

---

## Pattern 4 — Singleton

### Intent
Ensure a class has only one instance and provide a global access point to it.

### Interview Reality
> Interviewers ask about Singleton primarily to test **thread-safety knowledge**, not the pattern itself.

### The Four Implementations (Know All Four)

#### 1. Broken (show you know what NOT to do)
```java
// BROKEN: Race condition between null-check and assignment
public static Singleton getInstance() {
    if (instance == null) {           // Thread A checks null → true
        instance = new Singleton();   // Thread B also checks null → true
    }                                 // Both create instances = not singleton
    return instance;
}
```

#### 2. Synchronized method (simple, but slow)
```java
public static synchronized Singleton getInstance() {
    if (instance == null) {
        instance = new Singleton();
    }
    return instance;
}
// Problem: Every call acquires the lock, even after instance is created
```

#### 3. Double-Checked Locking with volatile (common production pattern)
```java
private static volatile Singleton instance;  // volatile is CRITICAL

public static Singleton getInstance() {
    if (instance == null) {                   // First check (no lock)
        synchronized (Singleton.class) {
            if (instance == null) {           // Second check (with lock)
                instance = new Singleton();
            }
        }
    }
    return instance;
}
// volatile prevents instruction reordering during object creation
// Without volatile, partially constructed object can be seen by other threads
```

#### 4. Bill Pugh / Initialization-on-demand (cleanest)
```java
public class Singleton {
    private Singleton() {}

    private static class Holder {
        private static final Singleton INSTANCE = new Singleton();
    }

    public static Singleton getInstance() {
        return Holder.INSTANCE;  // Class loaded lazily, JVM guarantees thread-safety
    }
}
```

#### 5. Enum Singleton (Josh Bloch recommendation, handles serialization)
```java
public enum Singleton {
    INSTANCE;
    public void doWork() { ... }
}
// Handles: thread-safety, serialization, reflection attacks
// Downside: can't extend, less flexible
```

### When NOT to Use Singleton
- When the "single instance" is really just shared configuration → use Spring `@Bean(singleton=true)` instead
- When it makes testing hard (hard to inject mock) → use DIP + IoC container instead
- The Singleton pattern is often a **design smell** — prefer dependency injection

### Common Mistakes
1. Not using `volatile` with double-checked locking (classic bug)
2. Not handling serialization (use `readResolve()` or Enum)
3. Using Singleton when Spring/DI framework manages lifecycle for you

---

## Pattern 5 — State

### Intent
Allow an object to alter its behavior when its internal state changes.
The object will appear to change its class.

### When to Use
- Object behavior depends on its state and must change at runtime
- You have large conditionals (`if/else`/`switch`) that check state everywhere
- State transitions are complex

### Interview Scenario
> "Design an order management system with states: PENDING → CONFIRMED → SHIPPED → DELIVERED → CANCELLED"

### State vs Strategy
| | State | Strategy |
|---|---|---|
| **Who changes behavior** | State object transitions to next state | Client injects the strategy |
| **Context awareness** | State knows about context (to transition) | Strategy usually doesn't know context |
| **Transitions** | Defined inside state classes | Client decides which strategy to use |

### Common Mistakes
1. Putting all state logic in the context class (defeats the purpose)
2. Forgetting that states can transition to other states
3. Using State pattern for simple 2-3 state machines (overkill — use a boolean or enum)

---

## Pattern 6 — Builder

### Intent
Separate the construction of a complex object from its representation,
allowing the same construction process to create different representations.

### When to Use
- Object has many optional parameters (telescoping constructor problem)
- Construction requires multiple steps in a specific order
- You want immutable objects with readable construction

### The Problem (Telescoping Constructor Anti-Pattern)
```java
// Which parameter is which? Is the 3rd arg timeout or retries?
HttpRequest req = new HttpRequest("GET", "http://api.com", 30, 3, true, null, "Bearer xyz");
```

### The Fix
```java
HttpRequest req = HttpRequest.builder()
    .method("GET")
    .url("http://api.com")
    .timeoutSeconds(30)
    .retries(3)
    .followRedirects(true)
    .authToken("Bearer xyz")
    .build();
```

### Builder vs Constructor with Named Parameters
- Java doesn't have named parameters → Builder is the idiomatic solution
- Kotlin/Python: named parameters reduce the need for Builder

### Lombok @Builder
In production Java, `@Builder` annotation generates this automatically.
In interviews, you implement it manually to show understanding.

---

## Pattern 7 — Decorator

### Intent
Attach additional responsibilities to an object dynamically.
Decorators provide a flexible alternative to subclassing for extending functionality.

### When to Use
- You want to add behavior to individual objects without affecting others
- Subclassing would lead to an explosion of classes
- Behaviors should be composable

### Classic Example: Java I/O Streams
```java
// This IS the Decorator pattern in the JDK
InputStream in = new BufferedInputStream(
                     new GZIPInputStream(
                         new FileInputStream("data.gz")));
```

### Interview Scenario
> "Add logging and caching to a data service without modifying the service class."

### The Violation
```java
// BAD: Subclass explosion — what if you want only caching? Only logging?
// What about caching + logging? That's 4 classes for 2 features.
class LoggingAndCachingDatabaseService extends DatabaseService { ... }
```

### The Fix
```java
// Each decorator wraps the same interface
DataService base     = new DatabaseService();
DataService cached   = new CachingDecorator(base);
DataService logged   = new LoggingDecorator(cached);  // compose as needed
```

### Decorator vs Inheritance
| | Decorator | Inheritance |
|---|---|---|
| **Composition** | Dynamic, at runtime | Static, compile-time |
| **Combinations** | N behaviors = N classes | N behaviors = 2^N subclasses |
| **Open for extension** | Yes — add new decorators | Requires touching hierarchy |

---

## Critical Pattern Combinations (For Complex LLD Problems)

### Strategy + Factory
Used when: "Create the right strategy based on configuration"
```
Factory creates → Strategy is injected into → Context
```
Example: `PaymentStrategyFactory.create("UPI")` returns `UPIStrategy`, injected into `CheckoutService`

### Strategy + Observer
Used when: "Notify different systems using different notification strategies"
```
Subject notifies → Observer that holds → Strategy for delivery
```

### State + Observer
Used when: "Notify subscribers when order state changes"
```
Order (Context) → State transition → notifyObservers() → EmailObserver, SMSObserver
```

---

## Pattern Anti-Patterns — What Interviewers Watch For

| Anti-Pattern | What It Looks Like | Fix |
|---|---|---|
| **Over-engineering** | Adding factory/strategy for 2 hardcoded cases | Use `new` directly if you only have 2 fixed types |
| **Singleton Abuse** | Wrapping stateless utility classes | Static methods or DI-managed beans |
| **Anemic Strategy** | Strategy with only one method, same as lambda | Use `Function<T,R>` lambda instead |
| **Leaky Observer** | Observers never unsubscribe | Lifecycle management / WeakReference |
| **Fat Builder** | Builder doing business logic | Builder only constructs, never validates business rules |

---

## Interview Mental Checklist

When designing a system, ask:
1. **Behavior varies?** → Strategy
2. **One-to-many updates?** → Observer
3. **Hide construction complexity?** → Factory / Builder
4. **Single instance needed?** → Singleton (but prefer DI)
5. **State-dependent behavior?** → State
6. **Add behavior without subclassing?** → Decorator
7. **Tree/recursive structure?** → Composite

---

## Summary Card

| Pattern | One-liner | Key Class Relationship |
|---|---|---|
| **Strategy** | Swap algorithms at runtime | Context *has-a* Strategy interface |
| **Observer** | Broadcast state changes | Subject *has-a* list of Observers |
| **Factory** | Encapsulate object creation | Factory *creates* Product interface |
| **Singleton** | Guarantee one instance | Private constructor + static accessor |
| **State** | Behavior changes with state | Context *has-a* State, State *knows* Context |
| **Builder** | Readable complex construction | Builder *builds* immutable Product |
| **Decorator** | Stack behaviors dynamically | Decorator *wraps* same interface |
