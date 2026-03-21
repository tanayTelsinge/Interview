package Day4_problems.elevator_system.code.strategies;

import Day4_problems.elevator_system.code.domain.Elevator;
import Day4_problems.elevator_system.code.domain.Request;
import Day4_problems.elevator_system.code.enums.ElevatorStatus;

import java.util.Comparator;
import java.util.List;

/**
 * Selects the elevator whose current floor is closest to the requested floor.
 * Elevators in MAINTENANCE are excluded.
 */
public class NearestElevatorStrategy implements ElevatorSelectionStrategy {

    @Override
    public Elevator selectElevator(List<Elevator> elevators, Request request) {
        return elevators.stream()
                .filter(e -> e.getStatus() != ElevatorStatus.MAINTENANCE)
                .min(Comparator.comparingInt(
                        e -> Math.abs(e.getCurrentFloor() - request.getSourceFloor())))
                .orElseThrow(() -> new RuntimeException("No available elevator"));
    }
}
