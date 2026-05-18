package state.good;

public class ConfirmedState implements OrderState {

    @Override
    public void confirm(Order order) {
        System.out.println("INFO: Order " + order.getOrderId() + " is already confirmed.");
    }

    @Override
    public void ship(Order order) {
        System.out.println("Order " + order.getOrderId() + ": Dispatched to courier. Tracking activated.");
        order.setState(new ShippedState());
    }

    @Override
    public void deliver(Order order) {
        System.out.println("ERROR: Cannot deliver order " + order.getOrderId() + " — not yet shipped.");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("Order " + order.getOrderId() + ": Cancelled after confirmation. Partial refund.");
        order.setState(new CancelledState());
    }

    @Override
    public String getStateName() {
        return "CONFIRMED";
    }
}
