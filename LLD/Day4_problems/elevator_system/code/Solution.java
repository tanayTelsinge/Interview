package Day4_problems.elevator_system.code;

import Day4_problems.elevator_system.code.domain.Elevator;
import Day4_problems.elevator_system.code.enums.Direction;
import Day4_problems.elevator_system.code.service.ElevatorController;
import Day4_problems.elevator_system.code.strategies.NearestElevatorStrategy;

import java.util.List;

public class Solution {

    public static void main(String[] args) {

        // Building: floors 1–10, three elevators
        Elevator e1 = new Elevator("E1", 1);   // starts at floor 1
        Elevator e2 = new Elevator("E2", 8);   // starts at floor 8
        Elevator e3 = new Elevator("E3", 4);   // starts at floor 4

        ElevatorController controller = new ElevatorController(
                List.of(e1, e2, e3),
                new NearestElevatorStrategy()
        );

        controller.printStatus();

        // ── External requests (floor panel presses) ──────────────────────
        System.out.println("\n=== External Requests ===");

        // Person on floor 3 wants to go UP  → nearest is E3 (dist 1)
        controller.requestElevator(3, Direction.UP);

        // Person on floor 6 wants to go DOWN → nearest is E3 (dist 2) or E2 (dist 2) — tie, first wins
        controller.requestElevator(6, Direction.DOWN);

        // Person on floor 9 wants to go DOWN → nearest is E2 (dist 1)
        controller.requestElevator(9, Direction.DOWN);

        controller.printStatus();

        // ── Simulate movement: drive each elevator to its first stop ──────
        System.out.println("\n=== Step 1 ===");
        controller.step();   // E3→3, E2→9

        // ── Internal requests (inside-elevator button presses) ────────────
        System.out.println("\n=== Internal Requests (after pickup) ===");

        // Person picked up by E3 at floor 3 wants to go to floor 7
        controller.selectFloor(e3, 7);

        // Person picked up by E2 at floor 9 wants to go to floor 2
        controller.selectFloor(e2, 2);

        // ── Continue simulation ───────────────────────────────────────────
        System.out.println("\n=== Steps 2-5 ===");
        controller.step();   // E3→6(picked up), E2 continues down
        controller.step();   // E3→7, E2 continues down
        controller.step();   // E2 continues down
        controller.step();   // E2→2

        System.out.println();
        controller.printStatus();
    }
}
