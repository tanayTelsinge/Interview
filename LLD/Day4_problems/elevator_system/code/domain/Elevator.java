package Day4_problems.elevator_system.code.domain;

import Day4_problems.elevator_system.code.enums.Direction;
import Day4_problems.elevator_system.code.enums.ElevatorStatus;

import java.util.TreeSet;

/**
 * Models a single elevator car.
 *
 * Uses the SCAN (Look) algorithm:
 *  - upQueue   : floors above currentFloor, visited in ascending order  while going UP.
 *  - downQueue : floors below currentFloor, visited in descending order while going DOWN.
 *
 * When the current directional queue empties, the elevator reverses if the other
 * queue has pending floors; otherwise it becomes IDLE.
 */
public class Elevator {

    private final String elevatorId;
    private int currentFloor;
    private Direction direction;
    private ElevatorStatus status;

    // Floors to visit while going UP  (ascending)
    private final TreeSet<Integer> upQueue   = new TreeSet<>();
    // Floors to visit while going DOWN (we use last() for descending traversal)
    private final TreeSet<Integer> downQueue = new TreeSet<>();

    public Elevator(String elevatorId, int initialFloor) {
        this.elevatorId   = elevatorId;
        this.currentFloor = initialFloor;
        this.direction    = Direction.IDLE;
        this.status       = ElevatorStatus.IDLE;
    }

    // ------------------------------------------------------------------ //
    //  Public API (all mutating methods are synchronized for thread safety)
    // ------------------------------------------------------------------ //

    /** Called by ElevatorController to add a destination (external or internal request). */
    public synchronized void addDestination(int floor) {
        if (floor == currentFloor) {
            System.out.println("[" + elevatorId + "] already at floor " + floor);
            return;
        }
        if (floor > currentFloor) {
            upQueue.add(floor);
        } else {
            downQueue.add(floor);
        }
        if (status == ElevatorStatus.IDLE) {
            direction = (floor > currentFloor) ? Direction.UP : Direction.DOWN;
            status    = ElevatorStatus.MOVING;
        }
    }

    /**
     * Simulates moving to the next destination floor (one step at a time).
     * Call this repeatedly (e.g. from a scheduler) to drive the elevator.
     */
    public synchronized void processNextFloor() {
        if (status != ElevatorStatus.MOVING) return;

        if (direction == Direction.UP && !upQueue.isEmpty()) {
            currentFloor = upQueue.first();
            upQueue.remove(currentFloor);
            System.out.println("[" + elevatorId + "] arrived at floor " + currentFloor + " (UP)");
            if (upQueue.isEmpty()) {
                direction = downQueue.isEmpty() ? Direction.IDLE : Direction.DOWN;
                if (direction == Direction.IDLE) status = ElevatorStatus.IDLE;
            }

        } else if (direction == Direction.DOWN && !downQueue.isEmpty()) {
            currentFloor = downQueue.last();
            downQueue.remove(currentFloor);
            System.out.println("[" + elevatorId + "] arrived at floor " + currentFloor + " (DOWN)");
            if (downQueue.isEmpty()) {
                direction = upQueue.isEmpty() ? Direction.IDLE : Direction.UP;
                if (direction == Direction.IDLE) status = ElevatorStatus.IDLE;
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  Getters
    // ------------------------------------------------------------------ //

    public String          getElevatorId()   { return elevatorId; }
    public int             getCurrentFloor() { return currentFloor; }
    public Direction       getDirection()    { return direction; }
    public ElevatorStatus  getStatus()       { return status; }
}
