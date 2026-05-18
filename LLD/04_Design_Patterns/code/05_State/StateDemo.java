package state;

import state.good.Order;

/**
 * Demo: State Pattern
 *
 * The Order context class has NO if/else — all logic lives in State classes.
 */
public class StateDemo {

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("   STATE PATTERN DEMO");
        System.out.println("========================================\n");

        // --- Happy path: PENDING → CONFIRMED → SHIPPED → DELIVERED ---
        System.out.println("--- Happy Path ---");
        Order order1 = new Order("ORD-001");
        System.out.println("Current state: " + order1.getCurrentStateName());
        order1.confirm();
        order1.ship();
        order1.deliver();
        System.out.println("Final state: " + order1.getCurrentStateName());

        // --- Cancel before shipping ---
        System.out.println("\n--- Cancel before shipping ---");
        Order order2 = new Order("ORD-002");
        order2.confirm();
        order2.cancel();
        order2.ship();  // Invalid — should print error

        // --- Invalid transitions ---
        System.out.println("\n--- Invalid transitions ---");
        Order order3 = new Order("ORD-003");
        order3.ship();    // Can't ship PENDING
        order3.deliver(); // Can't deliver PENDING
        order3.confirm();
        order3.deliver(); // Can't deliver CONFIRMED (not yet shipped)

        // --- Can't cancel shipped order ---
        System.out.println("\n--- Can't cancel after shipping ---");
        Order order4 = new Order("ORD-004");
        order4.confirm();
        order4.ship();
        order4.cancel();  // Should explain return process

        System.out.println("\n--- Interview takeaway ---");
        System.out.println("Order.java has zero if/else. Each state owns its own transitions.");
        System.out.println("Adding RETURN_REQUESTED state = add ReturnRequestedState.java only.");
    }
}
