# SOLID Principles — Day 2

> Senior interviewers NEVER ask "explain SRP". They give you a design problem and evaluate
> whether you naturally apply SOLID. Balance SOLID with KISS and YAGNI — don't over-engineer.

---

## S — Single Responsibility Principle (SRP)

**Rule:** A class should have one reason to change.

**How it's tested:** Interviewer sees your `BookingService` handling booking, payment, notification,
and cancellation all in one class. Then asks: "what if we change how fees are calculated?"
If the answer is "modify BookingService" — SRP is violated.

**The fix — composition and delegation:**
```
BookingService → delegates to → PaymentService
                              → NotificationService
                              → FeeCalculator
```

**Interview tip:** Look for classes with names like `Manager`, `Handler`, `Processor` doing too much.
A class name with "And" in it is a red flag: `PaymentAndNotificationService`.

**Java example:**
```java
// BAD
class BookingService {
    void book() { ... }
    void processPayment() { ... }   // payment responsibility
    void sendEmail() { ... }         // notification responsibility
    void generateReport() { ... }   // reporting responsibility
}

// GOOD
class BookingService {
    private final PaymentService payment;
    private final NotificationService notifications;
    void book() {
        payment.charge(...);
        notifications.sendConfirmation(...);
    }
}
```

---

## O — Open/Closed Principle (OCP)

**Rule:** Open for extension, closed for modification.

**How it's tested:** The extensibility probe — "add a new payment method / vehicle type / notification channel."
If your answer requires modifying existing code → OCP violated.

**The mechanism:** Interfaces + polymorphism. New behaviour = new class.

**Red flags interviewers watch for:**
- `if/else` chains with type checks
- `instanceof` checks
- `switch(type)` blocks

**Interview tip:** This is the most directly tied principle to design patterns.
OCP violations are why Strategy, Factory, and Observer exist.

```java
// BAD — adding PayTM means modifying this method
void processPayment(String type, double amount) {
    if (type.equals("UPI")) { ... }
    else if (type.equals("CARD")) { ... }
    // adding PAYTM = modify this = risky
}

// GOOD — adding PayTM = new class only
interface PaymentMethod { PaymentResult process(double amount); }
class UPIPayment implements PaymentMethod { ... }
class CardPayment implements PaymentMethod { ... }
class PayTMPayment implements PaymentMethod { ... }  // new class, zero existing code changed
```

---

## L — Liskov Substitution Principle (LSP)

**Rule:** Subtypes must be substitutable for their base types without breaking the program.

**How it's tested:** The Bird-Penguin trap. If your subclass throws `UnsupportedOperationException`
for a parent method, LSP is violated. The fix isn't at the method level — it's a **hierarchy problem**.

**Classic violations:**
| Violation | Fix |
|---|---|
| `Penguin extends Bird`, `fly()` throws exception | `FlyingBird` interface, only flying birds implement |
| `Square extends Rectangle`, `setWidth` changes both dimensions | Use `Shape` interface, no inheritance |
| `ElectricVehicle extends Vehicle`, `refuel()` not applicable | Interface segregation |

**Detection pattern:** If you find yourself writing `throw new UnsupportedOperationException()` in a subclass,
that's an LSP violation signal.

```java
// BAD
class Penguin extends Bird {
    @Override
    void fly() { throw new UnsupportedOperationException("Penguins can't fly!"); }
}

// GOOD — restructure the hierarchy
interface FlyingBird { void fly(); }
class Eagle extends Bird implements FlyingBird { public void fly() { ... } }
class Penguin extends Bird { }  // no fly() — correct
```

---

## I — Interface Segregation Principle (ISP)

**Rule:** Clients should not be forced to depend on methods they don't use.

**How it's tested:** Fat interfaces that force implementors to stub out empty methods.

**Detection pattern:** If you're writing `// not applicable` or empty implementations, ISP is violated.

**The fix:** Role-based interfaces — split by what the client needs, not by what the object can do.

```java
// BAD — fat interface
interface Worker {
    void work();
    void eat();    // Robots don't eat
    void sleep();  // Robots don't sleep
}
class Robot implements Worker {
    void work() { ... }
    void eat() { }    // forced empty implementation
    void sleep() { }  // forced empty implementation
}

// GOOD — role-based interfaces
interface Workable { void work(); }
interface Feedable { void eat(); }
interface Restable { void sleep(); }

class Robot implements Workable { void work() { ... } }
class HumanWorker implements Workable, Feedable, Restable { ... }
```

**Don't over-split:** One method per interface everywhere violates KISS. Aim for **cohesive, role-based** interfaces.

---

## D — Dependency Inversion Principle (DIP)

**Rule:** High-level modules should not depend on low-level modules. Both should depend on abstractions.

**How it's tested:** Interviewer looks for `new ConcreteClass()` inside service classes.

**Detection pattern:**
- `new InMemoryDatabase()` inside `ParkingLotService` → DIP violated
- `new RazorpayGateway()` inside `PaymentService` → DIP violated

**The fix:** Constructor injection of interface types.

```java
// BAD — high-level module hardcoded to low-level implementation
class ParkingLotService {
    private InMemoryParkingRepository repo = new InMemoryParkingRepository(); // hardcoded!
}

// GOOD — depends on abstraction, implementation injected
class ParkingLotService {
    private final ParkingRepository repo;  // interface

    public ParkingLotService(ParkingRepository repo) {  // injected
        this.repo = repo;
    }
}
// Now: can inject InMemoryRepo in tests, DatabaseRepo in production
```

**Spring Boot:** `@Autowired` constructor injection of interface types is DIP in practice.
The IoC container resolves the implementation — your service class never knows the concrete type.

---

## SOLID Summary Card

| Principle | One-liner | Most common violation | Fix |
|---|---|---|---|
| SRP | One reason to change | God class with too many responsibilities | Delegate to focused classes |
| OCP | Extend, don't modify | if/else / switch on type | Interface + polymorphism |
| LSP | Subtype = substitutable | throw UnsupportedOperationException in subclass | Restructure hierarchy |
| ISP | No fat interfaces | Empty/stub implementations in implementor | Role-based interfaces |
| DIP | Depend on abstractions | `new ConcreteClass()` in service | Constructor injection |

---

## Interview Pattern — How to Apply SOLID Naturally

When you design a class, run this mental checklist:

1. **SRP check:** "If requirement X changes, which class changes?" → should be one class.
2. **OCP check:** "If I add a new type of X, do I modify existing code?" → answer should be no.
3. **LSP check:** "Can every subclass be used wherever the parent is expected?" → yes, or fix hierarchy.
4. **ISP check:** "Are all implementors of this interface using all its methods?" → if not, split.
5. **DIP check:** "Is this class creating its own dependencies with `new`?" → inject instead.

### Balancing SOLID with KISS/YAGNI:
- Don't add a Factory if you have one implementation.
- Don't create a Strategy if there's only one algorithm.
- Apply OCP when the interviewer asks "how would you extend this?" — not before.
- Senior signal: "I'll keep this simple for now; here's how I'd extend it if needed."
