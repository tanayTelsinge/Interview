package Day4_problems.elevator_system.code.service;

import Day4_problems.elevator_system.code.domain.Elevator;
import Day4_problems.elevator_system.code.domain.Request;
import Day4_problems.elevator_system.code.enums.Direction;
import Day4_problems.elevator_system.code.strategies.ElevatorSelectionStrategy;

import java.util.List;

/**
 * Central dispatcher that manages a fleet of elevators.
 *
 * Two entry points:
 *  1. requestElevator() — external request: a person on a floor presses UP/DOWN.
 *     The strategy picks the best elevator and sends it to that floor.
 *  2. selectFloor()     — internal request: a person already inside an elevator
 *     presses a destination floor button.
 *
 * step() advances every elevator one floor along its queue (call in a loop or scheduler).
 */
public class ElevatorController {

    private final List<Elevator> elevators;
    private final ElevatorSelectionStrategy selectionStrategy;

    public ElevatorController(List<Elevator> elevators, ElevatorSelectionStrategy selectionStrategy) {
        this.elevators         = elevators;
        this.selectionStrategy = selectionStrategy;
    }

    // ------------------------------------------------------------------ //
    //  External request: floor panel button press
    // ------------------------------------------------------------------ //

    public void requestElevator(int sourceFloor, Direction direction) {
        Request request  = new Request(sourceFloor, direction);
        Elevator chosen  = selectionStrategy.selectElevator(elevators, request);
        System.out.println("Dispatching " + chosen.getElevatorId()
                + " (currently at floor " + chosen.getCurrentFloor() + ")"
                + " to floor " + sourceFloor + " [" + direction + "]");
        chosen.addDestination(sourceFloor);
    }

    // ------------------------------------------------------------------ //
    //  Internal request: inside-elevator button press
    // ------------------------------------------------------------------ //

    public void selectFloor(Elevator elevator, int destinationFloor) {
        System.out.println("Inside " + elevator.getElevatorId()
                + ": button pressed for floor " + destinationFloor);
        elevator.addDestination(destinationFloor);
    }

    // ------------------------------------------------------------------ //
    //  Simulation tick: advance every elevator one step
    // ------------------------------------------------------------------ //

    public void step() {
        System.out.println("--- tick ---");
        for (Elevator e : elevators) {
            e.processNextFloor();
        }
    }

    // ------------------------------------------------------------------ //
    //  Status dump
    // ------------------------------------------------------------------ //

    public void printStatus() {
        System.out.println("=== Elevator Status ===");
        for (Elevator e : elevators) {
            System.out.println(e.getElevatorId()
                    + "  floor=" + e.getCurrentFloor()
                    + "  dir="    + e.getDirection()
                    + "  status=" + e.getStatus());
        }
    }
}
