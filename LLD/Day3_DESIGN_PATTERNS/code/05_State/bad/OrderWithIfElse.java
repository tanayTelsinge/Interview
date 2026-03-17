package state.bad;

/**
 * VIOLATION: State pattern NOT applied.
 *
 * Problems:
 * 1. All state-dependent behavior lives in ONE class — massive if/else chains.
 * 2. Adding a new state (e.g., RETURN_REQUESTED) requires opening this class
 *    and modifying every method that checks state — shotgun surgery + OCP violation.
 * 3. Invalid state transitions are hard to prevent (nothing stops calling
 *    ship() on a DELIVERED order).
 * 4. As states grow, each method becomes a tower of if/else.
 */
public class OrderWithIfElse {

    private String state = "PENDING";  // BAD: magic string, no type safety
    private String orderId;

    public OrderWithIfElse(String orderId) {
        this.orderId = orderId;
    }

    // BAD: Every method is a state machine expressed as if/else
    public void confirm() {
        if (state.equals("PENDING")) {
            state = "CONFIRMED";
            System.out.println("Order " + orderId + " confirmed.");
        } else {
            System.out.println("Cannot confirm order in state: " + state);
        }
    }

    public void ship() {
        if (state.equals("CONFIRMED")) {
            state = "SHIPPED";
            System.out.println("Order " + orderId + " shipped.");
        } else {
            System.out.println("Cannot ship order in state: " + state);
        }
    }

    public void deliver() {
        if (state.equals("SHIPPED")) {
            state = "DELIVERED";
            System.out.println("Order " + orderId + " delivered.");
        } else {
            System.out.println("Cannot deliver order in state: " + state);
        }
    }

    public void cancel() {
        // Cancel rules differ by state — this gets complicated fast
        if (state.equals("PENDING") || state.equals("CONFIRMED")) {
            state = "CANCELLED";
            System.out.println("Order " + orderId + " cancelled.");
        } else if (state.equals("SHIPPED")) {
            System.out.println("Cannot cancel: order already shipped. Request return instead.");
        } else {
            System.out.println("Cannot cancel order in state: " + state);
        }
    }

    // Adding "RETURN_REQUESTED" state means modifying ALL of these methods above
    // and adding new if-else branches everywhere. Classic OCP violation.
}
