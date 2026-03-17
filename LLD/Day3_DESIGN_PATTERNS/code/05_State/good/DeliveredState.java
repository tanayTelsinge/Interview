package state.good;

public class DeliveredState implements OrderState {

    @Override
    public void confirm(Order order) {
        System.out.println("INFO: Order " + order.getOrderId() + " is already delivered.");
    }

    @Override
    public void ship(Order order) {
        System.out.println("INFO: Order " + order.getOrderId() + " is already delivered.");
    }

    @Override
    public void deliver(Order order) {
        System.out.println("INFO: Order " + order.getOrderId() + " is already marked as delivered.");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("ERROR: Order " + order.getOrderId() +
            " already delivered. Initiate return within 7 days.");
    }

    @Override
    public String getStateName() {
        return "DELIVERED";
    }
}
