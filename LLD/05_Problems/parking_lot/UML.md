# Parking Lot — UML Class Diagram

> Matches the actual implementation in 05_Problems/parking_lot/code/
> This is what you'd sketch on a whiteboard in the first 10 minutes.

---

## Class Diagram

```
«enumeration»            «enumeration»           «enumeration»
VehicleType              SpotType                SpotStatus
──────────               ──────────              ──────────
BIKE                     COMPACT                 AVAILABLE
CAR                      MEDIUM                  OCCUPIED
TRUCK                    LARGE


┌──────────────────────┐
│  «abstract»          │
│      Vehicle         │
├──────────────────────┤
│ - vehicleNumber      │
│ - vehicleType        │
├──────────────────────┤
│ + getVehicleNumber() │
│ + getVehicleType()   │
└──────────────────────┘
         ▲
         │ extends (inheritance)
    ┌────┴──────┬──────────┐
  Bike         Car        Truck


┌──────────────────────────┐
│      ParkingSpot          │
├──────────────────────────┤
│ - spotNumber: int         │
│ - floorNumber: int        │
│ - spotType: SpotType      │
│ - spotStatus: SpotStatus  │
│ - parkedVehicle: Vehicle  │
├──────────────────────────┤
│ + park(vehicle)           │
│ + vacateSpot()            │
│ + getSpotStatus()         │
│ + getSpotType()           │
└──────────────────────────┘


┌──────────────────────────────────┐
│          ParkingFloor             │
├──────────────────────────────────┤
│ - floorNumber: int                │
│ - spots: List<ParkingSpot>  ◆    │  ← composition (spots cannot exist without floor)
├──────────────────────────────────┤
│ + getSpots()                      │
└──────────────────────────────────┘


┌───────────────────────────────────────────────────────┐
│                     ParkingLot                         │
├───────────────────────────────────────────────────────┤
│ - floors: List<ParkingFloor>                     ◆    │  ← composition
│ - spotAllocationStrategy: SpotAllocationStrategy      │  ← dependency
│ - ticketService: TicketService                        │  ← dependency
├───────────────────────────────────────────────────────┤
│ + parkVehicle(vehicle): Ticket                        │
│ + vacateSpot(ticket): void                            │
│ + getAvailableSpotsForVehicleType(type): long         │
└───────────────────────────────────────────────────────┘
          │ uses                       │ uses
          ↓                            ↓
┌──────────────────────────┐   ┌─────────────────────────────┐
│  «interface»              │   │       TicketService          │
│  SpotAllocationStrategy   │   ├─────────────────────────────┤
├──────────────────────────┤   │ - pricingStrategy            │
│ + allocateSpot(          │   ├─────────────────────────────┤
│     type, spots)         │   │ + generateTicket(           │
│   : ParkingSpot          │   │     vehicle, spot): Ticket  │
│ + getCompatibleSpotTypes(│   │ + calculateHourlyFee(       │
│     type): List<SpotType>│   │     ticket): double         │
└──────────────────────────┘   └─────────────────────────────┘
          ▲                                 │ uses
          │ implements                      ↓
┌──────────────────────────┐   ┌─────────────────────────────┐
│   NearestSpotStrategy     │   │  «interface»                │
├──────────────────────────┤   │   PricingStrategy           │
│ - vehicleSpotMap:         │   ├─────────────────────────────┤
│   Map<VehicleType,        │   │ + calculatePrice(           │
│       List<SpotType>>     │   │     hours): double          │
└──────────────────────────┘   └─────────────────────────────┘
                                            ▲
                                            │ implements
                               ┌─────────────────────────────┐
                               │   HourlyPricingStrategy      │
                               ├─────────────────────────────┤
                               │ - pricePerHour: double       │
                               └─────────────────────────────┘


┌─────────────────────────────────────┐
│               Ticket                 │
├─────────────────────────────────────┤
│ - ticketId: String                   │
│ - vehicleNumber: String              │
│ - floorNumber: int                   │
│ - spotNumber: int                    │
│ - entryTime: LocalDateTime           │
│ - exitTime: LocalDateTime            │
├─────────────────────────────────────┤
│ + getSpotNumber()                    │
│ + getFloorNumber()                   │
│ + getEntryDateTime()                 │
│ + getExitDateTime()                  │
└─────────────────────────────────────┘
```

---

## Relationships Summary

| From → To | Type | Why |
|---|---|---|
| ParkingLot → ParkingFloor | **Composition ◆** | Floors cannot exist without the lot |
| ParkingFloor → ParkingSpot | **Composition ◆** | Spots cannot exist without a floor |
| ParkingLot → SpotAllocationStrategy | **Dependency** | Uses interface, doesn't own it; injected via constructor |
| ParkingLot → TicketService | **Dependency** | Injected via constructor |
| TicketService → PricingStrategy | **Dependency** | Uses interface; injected via constructor |
| NearestSpotStrategy → SpotAllocationStrategy | **Realization ◁---** | implements interface |
| HourlyPricingStrategy → PricingStrategy | **Realization ◁---** | implements interface |
| Bike/Car/Truck → Vehicle | **Inheritance ▲** | IS-A relationship with shared state/behavior |

---

## VehicleType → SpotType Compatibility

```
BIKE  → COMPACT
CAR   → MEDIUM, LARGE     (prefers medium, falls back to large)
TRUCK → LARGE
```
This mapping lives in `NearestSpotStrategy` — NOT in Vehicle or ParkingSpot (OCP: add new vehicle type = new entry in map, zero changes elsewhere).

---

## Key Design Decisions to Mention in Interview

1. **Strategy pattern** for spot allocation — swap `NearestSpotStrategy` for any other algorithm without touching `ParkingLot`
2. **Strategy pattern** for pricing — swap `HourlyPricingStrategy` for flat/surge pricing without touching `TicketService`
3. **Composition** for Lot→Floor→Spot — spots have no meaning outside a lot
4. **Constructor injection** over Singleton — makes `ParkingLot` testable and avoids shared mutable static state
5. **Both floorNumber + spotNumber** needed to uniquely identify a spot at vacate time (spotNumber alone is not globally unique)
6. **ChronoUnit.HOURS.between()** for duration — `getHour()` subtraction is wrong across midnight/day boundaries
