package Day4_problems.elevator_system.code.strategies;

import Day4_problems.elevator_system.code.domain.Elevator;
import Day4_problems.elevator_system.code.domain.Request;

import java.util.List;

public interface ElevatorSelectionStrategy {
    /**
     * Picks the best elevator to service the given external request.
     *
     * @param elevators all elevators managed by the controller
     * @param request   the floor + direction requested by a user
     * @return the chosen Elevator (never null; throws if none available)
     */
    Elevator selectElevator(List<Elevator> elevators, Request request);
}
