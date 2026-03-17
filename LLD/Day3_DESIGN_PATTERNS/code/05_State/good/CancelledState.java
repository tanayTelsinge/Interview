package state.good;

public class CancelledState implements OrderState {

    @Override
    public void confirm(Order order) {
        System.out.println("ERROR: Order " + order.getOrderId() + " is cancelled. Place a new order.");
    }

    @Override
    public void ship(Order order) {
        System.out.println("ERROR: Order " + order.getOrderId() + " is cancelled.");
    }

    @Override
    public void deliver(Order order) {
        System.out.println("ERROR: Order " + order.getOrderId() + " is cancelled.");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("INFO: Order " + order.getOrderId() + " is already cancelled.");
    }

    @Override
    public String getStateName() {
        return "CANCELLED";
    }
}
