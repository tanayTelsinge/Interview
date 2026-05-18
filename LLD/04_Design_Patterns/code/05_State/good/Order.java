import state.good.OrderState;

/**
 * Context class — holds the current state and delegates to it.
 *
 * Order has NO if/else for state transitions.
 * It just calls currentState.confirm(this), currentState.ship(this), etc.
 * The state object decides what to do and whether to transition.
 *
 * SRP: Order manages order data; State manages transition logic.
 * OCP: Adding a new state = add a new State class, no changes to Order.
 */
public class Order {

    private final String orderId;
    private OrderState currentState;

    public Order(String orderId) {
        this.orderId = orderId;
        this.currentState = new PendingState();  // Initial state
    }

    // Called by State objects to transition to next state
    public void setState(OrderState newState) {
        System.out.println("  [State transition] " + currentState.getStateName()
            + " → " + newState.getStateName());
        this.currentState = newState;
    }

    // --- Public API — delegates to current state ---

    public void confirm() {
        currentState.confirm(this);
    }

    public void ship() {
        currentState.ship(this);
    }

    public void deliver() {
        currentState.deliver(this);
    }

    public void cancel() {
        currentState.cancel(this);
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCurrentStateName() {
        return currentState.getStateName();
    }
}
