package state.good;

/**
 * State: PENDING — initial state after order is placed.
 * Valid transitions: confirm(), cancel()
 * Invalid: ship(), deliver()
 */
public class PendingState implements OrderState {

    @Override
    public void confirm(Order order) {
        System.out.println("Order " + order.getOrderId() + ": Payment verified. Confirming order.");
        order.setState(new ConfirmedState());
    }

    @Override
    public void ship(Order order) {
        System.out.println("ERROR: Cannot ship order " + order.getOrderId() + " — not yet confirmed.");
    }

    @Override
    public void deliver(Order order) {
        System.out.println("ERROR: Cannot deliver order " + order.getOrderId() + " — not yet shipped.");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("Order " + order.getOrderId() + ": Cancelled before confirmation. Full refund.");
        order.setState(new CancelledState());
    }

    @Override
    public String getStateName() {
        return "PENDING";
    }
}
