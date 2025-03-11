# 🚀 SOLID Principles in the Parking Lot System

The **SOLID** principles help in designing **scalable, maintainable, and extendable** software. Below is how each principle applies to our **Parking Lot System**.

---

## **1️⃣ SRP - Single Responsibility Principle**
> **"A class should have only one reason to change."**

### ✅ **Applied in our code:**
Each class has a **single responsibility**:
- **`BaseVehicle`** → Handles `licensePlate` and common vehicle behavior.
- **`SportsCar`, `Truck`** → Define specific parking requirements.
- **`ParkingLotManager`** → Handles **parking logic** only.

### 🔴 **Bad Example (Violating SRP)**
```java
class SportsCar {
    private String licensePlate;
    
    public int getParkingSpaceRequired() {
        return 1;
    }

    public void parkVehicle() {  // ❌ Parking logic inside vehicle class
        System.out.println("Car parked!");
    }
}
```

### ✅ **Correct Example (Following SRP)**
```java
class ParkingLotManager {
    public boolean parkVehicle(Vehicle vehicle) {
        // ✅ This class is only responsible for parking
    }
}
```

---

## **2️⃣ OCP - Open/Closed Principle**
> **"Software entities should be open for extension but closed for modification."**

### ✅ **Applied in our code:**
- We can **add new vehicle types** (`Bus`, `Motorcycle`, etc.) **without modifying** existing code.
- **We do NOT modify `ParkingLotManager`** when a new vehicle type is introduced.

### 🔴 **Bad Example (Violating OCP)**
```java
class ParkingLotManager {
    public int getParkingSpace(Vehicle vehicle) {
        if (vehicle instanceof SportsCar) return 1;  // ❌ Adding new cases every time
        else if (vehicle instanceof Truck) return 2;
    }
}
```

### ✅ **Correct Example (Following OCP)**
```java
interface Vehicle {
    int getParkingSpaceRequired();  // ✅ Each vehicle defines its own space requirement
}
```

---

## **3️⃣ LSP - Liskov Substitution Principle**
> **"Subtypes must be substitutable for their base types."**

### ✅ **Applied in our code:**
We can replace **any `BaseVehicle` object** with a **subclass (`SportsCar`, `Truck`)**, and it will work without breaking the system.

### 🔴 **Bad Example (Violating LSP)**
```java
class ElectricTruck extends Truck {
    @Override
    public int getParkingSpaceRequired() {
        throw new UnsupportedOperationException(); // ❌ Breaks substitutability
    }
}
```

### ✅ **Correct Example (Following LSP)**
```java
class Bus extends BaseVehicle {
    public Bus(String licensePlate) {
        super(licensePlate);
    }

    @Override
    public int getParkingSpaceRequired() {
        return 3;  // ✅ Bus requires 3 parking spaces, but it's still a Vehicle
    }
}
```

---

## **4️⃣ ISP - Interface Segregation Principle**
> **"Clients should not be forced to depend on interfaces they do not use."**

### ✅ **Applied in our code:**
We keep **only essential methods** in the `Vehicle` interface.
- We don’t force **all vehicles** to implement unnecessary methods (e.g., `startEngine()` for bicycles).

### 🔴 **Bad Example (Violating ISP)**
```java
interface Vehicle {
    int getParkingSpaceRequired();
    void startEngine();  // ❌ Bicycles don’t have engines!
}
```

### ✅ **Correct Example (Following ISP)**
```java
interface Vehicle {
    int getParkingSpaceRequired();  // ✅ Only parking-related behavior
}
```

---

## **5️⃣ DIP - Dependency Inversion Principle**
> **"High-level modules should depend on abstractions, not concrete implementations."**

### ✅ **Applied in our code:**
- **`ParkingLotManager` depends on the `Vehicle` interface**, not on specific vehicle types (`SportsCar`, `Truck`).
- This allows us to **add new vehicle types without modifying `ParkingLotManager`**.

### 🔴 **Bad Example (Violating DIP)**
```java
class ParkingLotManager {
    private SportsCar car;  // ❌ Tightly coupled with concrete class
}
```

### ✅ **Correct Example (Following DIP)**
```java
class ParkingLotManager {
    public boolean parkVehicle(Vehicle vehicle) {  // ✅ Depends on abstraction
        int requiredSpaces = vehicle.getParkingSpaceRequired();
    }
}
```

---

## **🌟 Final Summary**
| **SOLID Principle** | **How We Followed It in Our Parking Lot System** |
|---------------------|--------------------------------------------------|
| **SRP (Single Responsibility)** | `ParkingLotManager` only handles parking, `Vehicle` handles vehicle logic. |
| **OCP (Open-Closed)** | Adding new vehicle types (`Bus`, `Bike`) does not modify existing code. |
| **LSP (Liskov Substitution)** | Any `Vehicle` subclass (`SportsCar`, `Truck`) can replace `BaseVehicle` without breaking code. |
| **ISP (Interface Segregation)** | The `Vehicle` interface only defines necessary behavior. |
| **DIP (Dependency Inversion)** | `ParkingLotManager` depends on `Vehicle` abstraction, not concrete implementations. |

---

## **🚀 Final Thoughts**
✅ **The Parking Lot System follows all SOLID principles.**  
✅ **Our code is modular, scalable, and easy to extend.**  
✅ **Adding new vehicle types requires no changes to existing logic!**
