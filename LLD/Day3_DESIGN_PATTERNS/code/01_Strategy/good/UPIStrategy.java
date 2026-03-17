package strategy.good;

/**
 * Concrete Strategy: UPI payment.
 * Zero processing fee, higher transaction limit.
 *
 * Adding this class required ZERO changes to CheckoutService — OCP in action.
 */
public class UPIStrategy implements PaymentStrategy {

    private final String upiId;

    public UPIStrategy(String upiId) {
        this.upiId = upiId;
    }

    @Override
    public void pay(double amount) {
        System.out.println("=== UPI Payment ===");
        System.out.println("UPI ID: " + upiId);
        System.out.println("Amount: ₹" + amount + " (no processing fee)");
        System.out.println("UPI Reference: UPI" + System.currentTimeMillis());
        System.out.println("Payment successful!");
    }

    @Override
    public String getPaymentMethodName() {
        return "UPI (" + upiId + ")";
    }

    @Override
    public double getTransactionLimit() {
        return 200000.0;  // ₹2L per transaction (NPCI limit)
    }
}
