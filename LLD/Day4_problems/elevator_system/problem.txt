Design an Elevator System

Q. How many elevators?
- Multiple (configurable fleet, e.g. 3 elevators).

Q. How many floors?
- Multiple floors (configurable, e.g. 10 floors).

Q. What requests need to be supported?
- External request: person on a floor presses UP or DOWN button.
- Internal request: person inside elevator presses a destination floor button.

Q. How should the elevator be dispatched?
- A strategy selects the best elevator (default: nearest idle/moving elevator).

Q. What movement algorithm does each elevator use?
- SCAN (Look) algorithm: service all floors in the current direction first, then reverse.

Q. Do we need to handle concurrency?
- Yes. addDestination() and processNextFloor() on Elevator are synchronized.

Q. Any out-of-service state?
- Yes. ElevatorStatus.MAINTENANCE — elevators in this state are excluded from dispatch.
