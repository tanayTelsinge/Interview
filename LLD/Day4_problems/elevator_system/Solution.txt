- Elevator: elevatorId, currentFloor, direction, status, upQueue (TreeSet), downQueue (TreeSet).
  - addDestination(floor): adds floor to upQueue or downQueue; sets status MOVING if IDLE.
  - processNextFloor(): advances one step using SCAN; reverses direction when one queue empties.
- Request: requestId (UUID), sourceFloor, direction — represents an external floor-panel press.
- ElevatorController: holds List<Elevator> + ElevatorSelectionStrategy.
  - requestElevator(floor, direction): external request — strategy picks elevator, adds floor to its queue.
  - selectFloor(elevator, floor): internal request — adds destination directly to chosen elevator.
  - step(): calls processNextFloor() on all elevators (simulation tick).
- ElevatorSelectionStrategy (interface): selectElevator(elevators, request).
- NearestElevatorStrategy: picks non-MAINTENANCE elevator with min |currentFloor - sourceFloor|.


Flow:

External request (floor panel)
 - requestElevator(sourceFloor, direction)
     - Creates Request(sourceFloor, direction)
     - NearestElevatorStrategy.selectElevator() filters MAINTENANCE, picks min-distance elevator
     - elevator.addDestination(sourceFloor) → queued in upQueue or downQueue; status → MOVING

Internal request (inside elevator)
 - selectFloor(elevator, destinationFloor)
     - elevator.addDestination(destinationFloor) → queued in appropriate queue

Movement (SCAN / Look algorithm)
 - direction == UP  → take first() from upQueue  (lowest floor above current)
 - direction == DOWN→ take last()  from downQueue (highest floor below current, visited descending)
 - Queue exhausted   → reverse direction if other queue non-empty, else IDLE


Design Patterns used:

- Strategy — ElevatorSelectionStrategy / NearestElevatorStrategy
  - Dispatcher logic is swappable without touching Elevator or ElevatorController
  - e.g. swap in DirectionalAffinityStrategy that prefers elevators already heading the right way


SOLID Principles:

- SRP  — Elevator handles movement; ElevatorController handles dispatch; Strategy handles selection
- OCP  — New selection strategies (e.g. load-balanced) added without modifying existing classes
- LSP  — Any ElevatorSelectionStrategy implementation is substitutable
- DIP  — ElevatorController depends on ElevatorSelectionStrategy interface, not NearestElevatorStrategy
- ISP  — ElevatorSelectionStrategy is a single-method interface; not over-specified


Key Design Points:

1. SCAN (Look) algorithm — upQueue and downQueue (both TreeSet) ensure floors are serviced in
   sorted order for each direction, minimising total travel distance.

2. Deadlock-safe synchronization — addDestination() and processNextFloor() are synchronized on
   the Elevator instance; two threads can safely submit requests concurrently.

3. Direction reversal — when the directional queue empties, the elevator checks the opposite
   queue before going IDLE, avoiding unnecessary idle trips.

4. Strategy pattern for dispatch — NearestElevatorStrategy is the default; swap in a smarter
   strategy (e.g. directional affinity) with zero changes to ElevatorController.


Known limitations / future scope:

- Concurrency: ElevatorController.requestElevator() is not synchronized; concurrent external
  requests could pick the same elevator. Fix: synchronize on controller or use AtomicReference.
- No passenger model: requests are floor numbers only; weight/capacity limits not modelled.
- No real-time scheduler: step() is called manually; production use would need a thread-per-elevator
  or a scheduled executor driving processNextFloor() at fixed intervals.
