package code.OCP.good;

/**
 * GOOD — OCP applied.
 *
 * PaymentProcessor is CLOSED for modification.
 * It works with the PaymentMethod abstraction — never needs to change
 * when a new payment type is added.
 *
 * Adding PayTM = create PayTMPayment implements PaymentMethod.
 * This class = zero changes.
 */
public class PaymentProcessor {

    // Works with abstraction — doesn't know or care about concrete types
    public void processPayment(PaymentMethod method, double amount) {
        double fee = method.calculateFee(amount);
        double total = amount + fee;
        System.out.println("Payment via " + method.getName()
                + " | Amount: " + amount + " | Fee: " + fee + " | Total: " + total);
        method.process(total);
    }
}

// Adding PayTM tomorrow = NEW CLASS ONLY, PaymentProcessor untouched
class PayTMPayment implements PaymentMethod {
    @Override public void process(double amount) { System.out.println("Processing PayTM: " + amount); }
    @Override public double calculateFee(double amount) { return amount * 0.005; }
    @Override public String getName() { return "PayTM"; }
}

class OCPDemo {
    public static void main(String[] args) {
        PaymentProcessor processor = new PaymentProcessor();

        processor.processPayment(new CreditCardPayment(), 1000.0);
        processor.processPayment(new UPIPayment("john@upi"), 500.0);
        processor.processPayment(new PayTMPayment(), 750.0);
        // processor is CLOSED — didn't modify it to add PayTM
    }
}
