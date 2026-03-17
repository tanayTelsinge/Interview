# UML Class Diagrams — LLD Interview Essentials

> In interviews you won't draw perfect UML. You'll sketch on a whiteboard.
> Goal: communicate your design clearly and fast. Know the 5 relationships cold.

---

## What is a Class Diagram?

A class diagram shows:
- **Classes** — their name, fields, methods
- **Relationships** — how classes connect to each other

That's it. Don't over-complicate it.

---

## Class Box Syntax

```
┌─────────────────────┐
│      ClassName      │  ← Class name (bold/centered)
├─────────────────────┤
│ - field: Type       │  ← Fields (attributes)
│ # protectedField    │
│ + publicField       │
├─────────────────────┤
│ + method(): Return  │  ← Methods
│ - privateMethod()   │
└─────────────────────┘
```

**Visibility symbols:**
| Symbol | Meaning   |
|--------|-----------|
| `+`    | public    |
| `-`    | private   |
| `#`    | protected |

**In interviews:** just write the class name + key fields/methods. Skip visibility symbols if time is short.

---

## The 5 Relationships You MUST Know

### 1. Association — "uses"
A class holds a reference to another. General relationship.

```
Customer ————————— Order
```
- Customer has an Order (or knows about it)
- Weakest relationship — just a reference

**Code:**
```java
class Customer {
    private Order order; // association
}
```

---

### 2. Aggregation — "has-a" (weak ownership)
One class contains another, but the contained object can exist independently.
Shown with a **hollow diamond** on the owner side.

```
ParkingLot ◇————— ParkingFloor
```
- ParkingLot has ParkingFloors
- But ParkingFloor can conceptually exist without a ParkingLot
- If ParkingLot is deleted, floors could still exist independently

**Code:**
```java
class ParkingLot {
    private List<ParkingFloor> floors; // aggregation
}
```

**Memory trick:** Hollow diamond = hollow/weak ownership

---

### 3. Composition — "has-a" (strong ownership)
One class owns another. The contained object CANNOT exist without the owner.
Shown with a **filled diamond** on the owner side.

```
Order ◆————— OrderItem
```
- Order owns OrderItems
- If Order is deleted, OrderItems are deleted too — they have no meaning alone
- OrderItem cannot exist without an Order

**Code:**
```java
class Order {
    private List<OrderItem> items = new ArrayList<>(); // composition
    // items are created inside Order, die with Order
}
```

**Memory trick:** Filled diamond = filled/strong ownership

---

### 4. Inheritance — "is-a"
Subclass inherits from parent. Shown with a **hollow arrow** pointing to parent.

```
Vehicle ◁———— Car
Vehicle ◁———— Truck
```

**Code:**
```java
class Car extends Vehicle { }
```

---

### 5. Realization (Interface Implementation) — "can-do"
Class implements an interface. Shown with a **dashed hollow arrow**.

```
PaymentMethod ◁- - - CreditCardPayment
PaymentMethod ◁- - - UPIPayment
```

**Code:**
```java
class CreditCardPayment implements PaymentMethod { }
```

---

## Aggregation vs Composition — The Most Confused Pair

| | Aggregation | Composition |
|---|---|---|
| Diamond | Hollow ◇ | Filled ◆ |
| Ownership | Weak | Strong |
| Child lifecycle | Independent | Dies with parent |
| Example | Team ◇— Player | Order ◆— OrderItem |
| Example | ParkingLot ◇— Floor | House ◆— Room |

**Interview shortcut:** Ask "if I delete the parent, does the child still make sense?"
- Yes → Aggregation
- No → Composition

---

## Multiplicity (Cardinality)

Shows how many instances participate in a relationship.

```
ParkingLot 1 ◇————— 1..* ParkingFloor
ParkingFloor 1 ◇————— 1..* ParkingSpot
Customer 1 ————————— 0..* Order
```

| Notation | Meaning         |
|----------|-----------------|
| `1`      | exactly one     |
| `0..1`   | zero or one     |
| `*`      | zero or more    |
| `1..*`   | one or more     |
| `2..5`   | between 2 and 5 |

---

## Dependency — "uses temporarily"
A class uses another only in a method (not as a field).
Shown with a **dashed arrow**.

```
ParkingLotService - - → FeeCalculator
```

```java
class ParkingLotService {
    public double calculateFee(FeeCalculator calc, Ticket ticket) { // dependency — not stored
        return calc.compute(ticket);
    }
}
```

---

## Quick Reference — All Relationships

```
Association     ————————     uses/knows about (field reference)
Aggregation     ◇————————    has-a, weak (child independent)
Composition     ◆————————    has-a, strong (child dies with parent)
Inheritance     ◁————————    is-a (extends)
Realization     ◁- - - - -   can-do (implements)
Dependency      - - - - - →  uses temporarily (method param)
```

---

## How to Draw in an Interview (Step by Step)

1. **List your classes** as boxes — name only first
2. **Add key fields** — just the important ones (id, status, type)
3. **Add key methods** — entry/exit points, not getters/setters
4. **Draw relationships** — start with inheritance/realization, then composition/aggregation
5. **Add multiplicity** — 1, 1..*, 0..*

**Time budget:** 5-10 minutes max. Sketch, don't perfect.

---

## Common Interview Mistake

Drawing inheritance when composition is correct:

```
❌ WRONG
Vehicle ◁———— Car ◁———— ElectricCar ◁———— TeslaModel3
(deep inheritance chain — fragile)

✅ RIGHT
Car ◆———— Engine (composed)
     ◆———— Battery (composed, for electric)
```

---

## What to Memorize

| Relationship | Symbol    | Code keyword  | Real example              |
|---|---|---|---|
| Association  | ————      | field ref     | Customer → Order          |
| Aggregation  | ◇————     | List<> field  | Team → Player             |
| Composition  | ◆————     | nested new    | Order → OrderItem         |
| Inheritance  | ◁————     | extends       | Car → Vehicle             |
| Realization  | ◁- - -    | implements    | UPIPayment → PaymentMethod|
| Dependency   | - - - →   | method param  | Service → Calculator      |
