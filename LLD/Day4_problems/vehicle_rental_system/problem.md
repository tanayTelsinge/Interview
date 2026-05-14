# Vehicle Rental System

## Requirements Clarification (Q&A format)

**Q. What types of vehicles can be rented?**
- Car, Bike, Truck.

**Q. Can customers browse available vehicles?**
- Yes, by vehicle type and location.

**Q. How is rental pricing calculated?**
- Daily rate per vehicle type. Discounts apply for rentals longer than 7 days.

**Q. Can a customer reserve a vehicle in advance?**
- Yes, a reservation has a start date and end date.

**Q. What are the rental statuses?**
- AVAILABLE, RESERVED, RENTED, UNDER_MAINTENANCE.

**Q. Is there a late return penalty?**
- Yes, a fixed daily surcharge per day beyond the agreed return date.

**Q. Can a single customer have multiple active reservations?**
- No. One active reservation at a time per customer.

**Q. Do we need user accounts / authentication?**
- Assume customers are pre-registered. No auth implementation needed.

---

## Core Entities

| Entity | Role |
|---|---|
| `Vehicle` | Abstract base — vehicleId, type, model, status |
| `Car / Bike / Truck` | Concrete vehicle subtypes |
| `Customer` | customerId, name, drivingLicenseNumber |
| `Reservation` | Links customer ↔ vehicle for a date range |
| `RentalInvoice` | Computed at return — base cost + late fee |
| `VehicleRentalSystem` | Orchestrator — reserve, return, search |
| `PricingStrategy` | Interface — calculates rental cost |
| `DailyRatePricingStrategy` | Default implementation with weekly discount |

---

## Class Design

```
«enumeration»              «enumeration»
VehicleType                VehicleStatus
──────────                 ──────────────
CAR                        AVAILABLE
BIKE                       RESERVED
TRUCK                      RENTED
                           UNDER_MAINTENANCE


┌──────────────────────────┐
│  «abstract»              │
│       Vehicle            │
├──────────────────────────┤
│ - vehicleId: String      │
│ - model: String          │
│ - vehicleType: VehicleType│
│ - status: VehicleStatus  │
├──────────────────────────┤
│ + getVehicleId()         │
│ + getVehicleType()       │
│ + getStatus()            │
│ + setStatus()            │
└──────────────────────────┘
         ▲
         │ extends
    ┌────┴──────┬──────────┐
   Car         Bike       Truck


┌──────────────────────────────────┐
│            Customer               │
├──────────────────────────────────┤
│ - customerId: String              │
│ - name: String                    │
│ - drivingLicenseNumber: String    │
├──────────────────────────────────┤
│ + getCustomerId()                 │
└──────────────────────────────────┘


┌─────────────────────────────────────┐
│             Reservation              │
├─────────────────────────────────────┤
│ - reservationId: String             │
│ - customer: Customer                │
│ - vehicle: Vehicle                  │
│ - startDate: LocalDate              │
│ - expectedReturnDate: LocalDate     │
│ - actualReturnDate: LocalDate       │
│ - status: ReservationStatus         │
├─────────────────────────────────────┤
│ + getDurationDays(): long           │
│ + isOverdue(): boolean              │
└─────────────────────────────────────┘

«enumeration»
ReservationStatus
──────────────────
ACTIVE
COMPLETED
CANCELLED


┌──────────────────────────────────────┐
│            RentalInvoice              │
├──────────────────────────────────────┤
│ - invoiceId: String                   │
│ - reservationId: String               │
│ - baseAmount: double                  │
│ - lateFee: double                     │
│ - totalAmount: double                 │
├──────────────────────────────────────┤
│ + getTotalAmount()                    │
└──────────────────────────────────────┘


┌───────────────────────────────────────────────────────┐
│                 VehicleRentalSystem                     │
├───────────────────────────────────────────────────────┤
│ - vehicles: Map<String, Vehicle>          ◆            │
│ - reservations: Map<String, Reservation>  ◆            │
│ - pricingStrategy: PricingStrategy                     │
├───────────────────────────────────────────────────────┤
│ + searchAvailableVehicles(type, start, end)            │
│   : List<Vehicle>                                      │
│ + reserveVehicle(customer, vehicleId,                  │
│     start, end): Reservation                           │
│ + returnVehicle(reservationId,                         │
│     actualReturnDate): RentalInvoice                   │
│ + cancelReservation(reservationId): void               │
└───────────────────────────────────────────────────────┘
          │ uses
          ↓
┌──────────────────────────────┐
│  «interface»                  │
│   PricingStrategy             │
├──────────────────────────────┤
│ + calculateBaseCost(          │
│     type, days): double       │
│ + calculateLateFee(           │
│     type, overdueDays): double│
└──────────────────────────────┘
          ▲
          │ implements
┌──────────────────────────────┐
│  DailyRatePricingStrategy     │
├──────────────────────────────┤
│ - dailyRates:                 │
│   Map<VehicleType, Double>   │
│ - weeklyDiscountRate: double  │
│ - lateFeeMultiplier: double   │
└──────────────────────────────┘
```

---

## Core Flows

### Reserve a Vehicle
```
customer calls reserveVehicle(customerId, vehicleId, startDate, endDate)
  1. Validate customer has no active reservation
  2. Check vehicle status == AVAILABLE
  3. Check no overlapping reservation for the vehicle in date range
  4. Create Reservation(ACTIVE), set vehicle status = RESERVED
  5. Store reservation, return Reservation object
```

### Return a Vehicle
```
customer calls returnVehicle(reservationId, actualReturnDate)
  1. Look up Reservation by ID
  2. Calculate days rented = ChronoUnit.DAYS.between(startDate, actualReturnDate)
  3. Calculate base cost via PricingStrategy.calculateBaseCost(type, days)
  4. If actualReturnDate > expectedReturnDate:
       overdueDays = DAYS.between(expectedReturnDate, actualReturnDate)
       lateFee = PricingStrategy.calculateLateFee(type, overdueDays)
  5. Build RentalInvoice, mark Reservation COMPLETED, set vehicle status = AVAILABLE
  6. Return invoice
```

### Search Available Vehicles
```
searchAvailableVehicles(type, startDate, endDate)
  → filter vehicles by type
  → exclude any vehicle with an ACTIVE reservation overlapping [startDate, endDate]
  → return vehicles with status AVAILABLE or RESERVED-but-not-overlapping
```

---

## Design Patterns Used

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `PricingStrategy` | Swap daily-rate for surge/flat pricing without touching `VehicleRentalSystem` |
| **Factory** | `VehicleFactory.create(type, ...)` | Creates the right Vehicle subclass from a type enum |
| **Builder** | `Reservation` or `RentalInvoice` | Multiple optional fields; Builder enforces construction validity |

---

## SOLID Principles Applied

| Principle | Application |
|---|---|
| **SRP** | `VehicleRentalSystem` orchestrates; `PricingStrategy` handles cost; `Reservation` holds state |
| **OCP** | New pricing model (surge, corporate) = new class implementing `PricingStrategy`, zero changes to system |
| **LSP** | `Car/Bike/Truck` are substitutable wherever `Vehicle` is used |
| **ISP** | `PricingStrategy` is focused (base cost + late fee); not bloated with unrelated ops |
| **DIP** | `VehicleRentalSystem` depends on `PricingStrategy` interface, not `DailyRatePricingStrategy` directly |

---

## Concurrency Considerations

- **Race condition**: two customers reserving the same vehicle simultaneously.
  - Fix: `synchronized` block or `ReentrantLock` around reserve/return; or use optimistic locking with a version field.
- **Vehicle status map**: use `ConcurrentHashMap` if accessed from multiple threads.
- **Proactively flag this** — fintech/mobility interviewers expect it.

---

## Extensibility Probes ("What if…")

| Follow-up | Design response |
|---|---|
| Add EV vehicle type | New `ElectricCar extends Vehicle`; no change to system |
| Add corporate discount | New `CorporatePricingStrategy implements PricingStrategy` |
| Add vehicle damage fee at return | Add `damageAssessment: double` field to `RentalInvoice`; `returnVehicle` accepts optional damage amount |
| Support multiple active reservations per customer | Remove single-reservation guard; update validation logic only |

---

## Known Limitations / Future Scope

- No payment processing — invoice is computed but not settled.
- No persistence — all state is in-memory maps.
- `searchAvailableVehicles` overlap check is O(n×r) — acceptable for in-memory; needs DB index in production.
