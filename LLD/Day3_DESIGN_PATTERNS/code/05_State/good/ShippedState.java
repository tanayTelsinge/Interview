package state.good;

public class ShippedState implements OrderState {

    @Override
    public void confirm(Order order) {
        System.out.println("INFO: Order " + order.getOrderId() + " already confirmed and shipped.");
    }

    @Override
    public void ship(Order order) {
        System.out.println("INFO: Order " + order.getOrderId() + " is already shipped.");
    }

    @Override
    public void deliver(Order order) {
        System.out.println("Order " + order.getOrderId() + ": Delivered successfully. Thank you!");
        order.setState(new DeliveredState());
    }

    @Override
    public void cancel(Order order) {
        // Once shipped, can't cancel — must request return instead
        System.out.println("ERROR: Cannot cancel order " + order.getOrderId() +
            " — already shipped. Please request a return after delivery.");
    }

    @Override
    public String getStateName() {
        return "SHIPPED";
    }
}
