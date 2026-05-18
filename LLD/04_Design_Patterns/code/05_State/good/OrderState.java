package state.good;

/**
 * State interface — each state implements valid transitions and behavior.
 *
 * Key design decision: State methods receive the Order (context) as a parameter,
 * allowing states to trigger transitions (order.setState(new ShippedState())).
 *
 * This keeps the Order class lean — it delegates all state-dependent behavior
 * to the current state object.
 */
public interface OrderState {

    void confirm(Order order);

    void ship(Order order);

    void deliver(Order order);

    void cancel(Order order);

    String getStateName();
}
