# Elevator System — UML Class Diagram

> Matches the actual implementation in 05_Problems/elevator_system/code/
> This is what you'd sketch on a whiteboard in the first 10 minutes.

---

## Enums

```
«enumeration»          «enumeration»
Direction              ElevatorStatus
─────────────          ──────────────
UP                     IDLE
DOWN                   MOVING
IDLE                   MAINTENANCE
```

---

## Class Diagram

```
┌────────────────────────────────────────────────────────┐
│                      Elevator                           │
├────────────────────────────────────────────────────────┤
│ - elevatorId : String                                   │
│ - currentFloor : int                                    │
│ - direction : Direction                                 │
│ - status : ElevatorStatus                              │
│ - upQueue : TreeSet<Integer>     ◆                     │  ← floors above, ascending
│ - downQueue : TreeSet<Integer>   ◆                     │  ← floors below, descending
├────────────────────────────────────────────────────────┤
│ + addDestination(floor): void         (synchronized)   │
│ + processNextFloor(): void            (synchronized)   │
│ + getElevatorId(): String                              │
│ + getCurrentFloor(): int                               │
│ + getDirection(): Direction                            │
│ + getStatus(): ElevatorStatus                          │
└────────────────────────────────────────────────────────┘


┌──────────────────────────────────────┐
│              Request                  │
├──────────────────────────────────────┤
│ - requestId : String  (UUID)          │
│ - sourceFloor : int                   │
│ - direction : Direction               │
├──────────────────────────────────────┤
│ + getRequestId(): String              │
│ + getSourceFloor(): int               │
│ + getDirection(): Direction           │
└──────────────────────────────────────┘


┌──────────────────────────────────────────────────────────┐
│                   ElevatorController                      │
├──────────────────────────────────────────────────────────┤
│ - elevators : List<Elevator>                              │
│ - selectionStrategy : ElevatorSelectionStrategy           │
├──────────────────────────────────────────────────────────┤
│ + requestElevator(sourceFloor, direction): void           │  ← external request
│ + selectFloor(elevator, destinationFloor): void           │  ← internal request
│ + step(): void                                            │  ← simulation tick
│ + printStatus(): void                                     │
└──────────────────────────────────────────────────────────┘
          │ uses
          ↓
┌──────────────────────────────────────────┐
│  «interface»                              │
│   ElevatorSelectionStrategy               │
├──────────────────────────────────────────┤
│ + selectElevator(                         │
│     elevators: List<Elevator>,            │
│     request: Request                      │
│   ): Elevator                             │
└──────────────────────────────────────────┘
          ▲
          │ implements
          │
  NearestElevatorStrategy
  (min |currentFloor - sourceFloor|,
   excludes MAINTENANCE)
```

---

## SCAN (Look) Algorithm

```
State:  upQueue = {7, 9},  downQueue = {3, 1},  currentFloor = 5,  direction = UP

Tick 1: direction=UP  → currentFloor = upQueue.first() = 7   │ upQueue = {9}
Tick 2: direction=UP  → currentFloor = upQueue.first() = 9   │ upQueue = {}  → switch DOWN
Tick 3: direction=DOWN→ currentFloor = downQueue.last() = 3  │ downQueue = {1}
Tick 4: direction=DOWN→ currentFloor = downQueue.last() = 1  │ downQueue = {} → IDLE
```

---

## Request Dispatch Flow

```
Person on floor 3 presses UP
        │
        ▼
ElevatorController.requestElevator(3, UP)
        │
        ▼  creates
    Request(sourceFloor=3, direction=UP)
        │
        ▼  delegates to
ElevatorSelectionStrategy.selectElevator(elevators, request)
        │
        ▼  returns chosen elevator
Elevator.addDestination(3)
        │  floor > currentFloor? → upQueue.add(3)
        │  status == IDLE?       → direction=UP, status=MOVING
        ▼
[subsequent step() calls drive elevator to floor 3]

Person inside elevator presses floor 7
        │
        ▼
ElevatorController.selectFloor(elevator, 7)
        │
        ▼
Elevator.addDestination(7) → upQueue.add(7)
```

---

## Relationships Summary

| From → To | Type | Why |
|---|---|---|
| ElevatorController → Elevator | **Aggregation ◇** | Controller manages the fleet; elevators exist independently |
| ElevatorController → ElevatorSelectionStrategy | **Dependency** | Strategy injected at construction; used per request |
| Elevator → upQueue / downQueue | **Composition ◆** | TreeSets are owned by and live inside Elevator |
| NearestElevatorStrategy → ElevatorSelectionStrategy | **Realization ◁---** | implements interface |
| ElevatorController → Request | **Dependency** | Creates Request as a short-lived value object |

---

## Key Design Points

1. **SCAN algorithm** — `upQueue` and `downQueue` (TreeSet) ensure floors are visited in sorted
   order per direction, minimising total travel. On queue exhaustion the elevator reverses rather
   than returning to floor 1, matching real elevator behaviour.

2. **Synchronized Elevator** — `addDestination()` and `processNextFloor()` are `synchronized`;
   multiple threads can submit requests without corrupting queue state.

3. **Strategy pattern for dispatch** — `NearestElevatorStrategy` is the default; swap in a
   directional-affinity or load-balanced strategy with zero changes to `ElevatorController`.

4. **External vs internal requests** — `requestElevator()` (floor panel) goes through the
   strategy; `selectFloor()` (inside car) bypasses it and targets the exact elevator directly.

5. **MAINTENANCE exclusion** — elevators in `MAINTENANCE` status are filtered out by the
   strategy, so broken cars are never dispatched.
