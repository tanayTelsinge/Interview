package code.OCP.good;

public class CreditCardPayment implements PaymentMethod {

    @Override
    public void process(double amount) {
        System.out.println("Processing credit card payment of " + amount);
        // card-specific: CVV check, network call, etc.
    }

    @Override
    public double calculateFee(double amount) {
        return amount * 0.02; // 2% fee
    }

    @Override
    public String getName() { return "Credit Card"; }
}
