# OOP Fundamentals — Day 1

> Interview reality: You won't be asked "what is encapsulation?".
> You'll get a design problem and be evaluated on whether you *apply* these concepts naturally.

---

## The 4 Pillars

### 1. Encapsulation
**What:** Bundle data + behaviour together. Hide internal state. Expose only what's needed.

**Why interviewers care:** Violations lead to bugs that are hard to trace. Senior candidates design with encapsulation by default.

**Common violations to avoid:**
- Returning mutable collections from getters → callers can modify internal state
- Public fields instead of private + getter/setter
- `order.getCustomer().getAddress().getZipCode()` → Law of Demeter violation

**Fix pattern:**
```java
// BAD — caller can mutate internal list
public List<Item> getItems() { return items; }

// GOOD — defensive copy or unmodifiable view
public List<Item> getItems() { return Collections.unmodifiableList(items); }
```

**Law of Demeter — "talk to friends, not strangers":**
```java
// BAD
order.getCustomer().getAddress().getZipCode();

// GOOD — order exposes what callers need
order.getShippingZipCode();
```

---

### 2. Abstraction
**What:** Expose *what* something does, hide *how* it does it.

**Why interviewers care:** Good abstractions make systems extensible. Bad abstractions leak implementation details and cause coupling.

**Key Java tools:** Interfaces + Abstract classes

**Interview pattern:** Define the contract (interface) first, worry about implementation later.
```java
// Contract — what it does
interface PaymentGateway {
    PaymentResult process(Payment payment);
}

// Implementation — how it does it (hidden from caller)
class RazorpayGateway implements PaymentGateway { ... }
class StripeGateway implements PaymentGateway { ... }
```

---

### 3. Inheritance
**What:** Child class inherits state and behaviour from parent.

**When to use (strict rule):**
- Genuine IS-A relationship
- Shared *stable* implementation (parent won't change often)
- Example: `ArrayList extends AbstractList` — textbook correct

**When NOT to use (senior trap):**
- `Car extends Engine` → Car IS-NOT-A Engine
- `Square extends Rectangle` → setWidth on a Square breaks Rectangle's invariant
- `Penguin extends Bird` with `fly()` throwing exception → LSP violation

**Default rule: PREFER COMPOSITION over inheritance.**

---

### 4. Polymorphism
**What:** Same interface, different behaviour at runtime.

**Two types:**
- **Compile-time (overloading):** same method name, different parameters
- **Runtime (overriding):** subclass provides its own implementation of parent method

**Why interviewers care:** Polymorphism is the mechanism that makes Strategy, Observer, and Factory patterns work. Without it, you end up with if/else chains.

```java
// Without polymorphism — OCP violation
if (type.equals("UPI")) { processUPI(); }
else if (type.equals("CARD")) { processCard(); }

// With polymorphism — extensible
PaymentMethod method = getPaymentMethod(type); // returns right impl
method.process(payment); // no if/else, add new method = new class only
```

---

## Abstract Class vs Interface — Decision Guide

| | Interface | Abstract Class |
|---|---|---|
| Multiple inheritance | Yes | No (single) |
| State (instance vars) | No | Yes |
| Constructor | No | Yes |
| Default implementation | Yes (default methods, Java 8+) | Yes |
| Use when | Capability contract | Shared implementation + state |

**JDK model to memorize:**
```
List (interface) → AbstractList (abstract, skeletal impl) → ArrayList (concrete)
```

**Practical heuristic:**
1. Start with an **interface** — always more flexible
2. If multiple classes share the same implementation → extract **abstract class** in the middle
3. Never jump straight to abstract class

**Classic examples:**
```java
// Interface — capability (CAN do something)
interface Drawable { void draw(); }
interface Serializable { byte[] serialize(); }

// Abstract class — shared state/template
abstract class Vehicle {
    private String licensePlate;    // shared state
    abstract void startEngine();    // subclass must implement
    void displayPlate() { ... }     // shared behaviour
}
```

---

## Composition vs Inheritance — The Most Important Decision

> "Favor composition over inheritance" — Joshua Bloch, Effective Java

### Why composition wins:
1. **Swap at runtime** — inject different implementations
2. **Avoids fragile base class** — changing parent doesn't ripple to children
3. **Testable** — mock composed objects easily
4. **No deep hierarchies** — simpler to understand

### The trap:
```java
// BAD — Car IS-NOT-A Engine
class Car extends Engine {
    void drive() { start(); }  // reusing Engine.start()
}

// GOOD — Car HAS-A Engine
class Car {
    private final Engine engine;  // injected
    Car(Engine engine) { this.engine = engine; }
    void drive() { engine.start(); }
}
```

### When inheritance is genuinely correct:
- `Dog extends Animal` — Dog IS-A Animal, shares eat/sleep behaviour
- `SavingsAccount extends Account` — genuine IS-A, stable shared state
- `ArrayList extends AbstractList` — skeletal implementation pattern

---

## Interview Traps — Senior Level

### Trap 1: Bird-Penguin (LSP)
```java
// BAD — Penguin IS-A Bird but can't fly
class Penguin extends Bird {
    void fly() { throw new UnsupportedOperationException(); } // LSP violated
}

// FIX — interface segregation
interface FlyingBird { void fly(); }
class Eagle extends Bird implements FlyingBird { ... }
class Penguin extends Bird { }  // no fly() — correct
```

### Trap 2: Rectangle-Square (LSP)
```java
// BAD — setWidth on Square silently changes both sides
class Square extends Rectangle {
    void setWidth(int w) { this.width = w; this.height = w; } // breaks Rectangle's contract
}
// FIX — don't inherit, use a Shape interface instead
```

### Trap 3: Returning mutable internal state
```java
// BAD
class Order {
    private List<Item> items = new ArrayList<>();
    public List<Item> getItems() { return items; } // caller can .clear() your list!
}

// GOOD
public List<Item> getItems() { return Collections.unmodifiableList(items); }
```

### Trap 4: Diamond problem (Java 8+ default methods)
```java
interface A { default void hello() { System.out.println("A"); } }
interface B { default void hello() { System.out.println("B"); } }

class C implements A, B {
    // Must resolve ambiguity explicitly
    public void hello() { A.super.hello(); }
}
```

---

## Quick Reference — Interview Checklist

Before designing any class, ask:
- [ ] Does this class have ONE clear responsibility?
- [ ] Am I using inheritance or composition? Is inheritance genuinely IS-A?
- [ ] Am I exposing internal state through getters that return mutable objects?
- [ ] Is my interface too fat? Can I split it into role-based interfaces?
- [ ] Am I creating concrete classes with `new` inside a service? (DIP violation)
