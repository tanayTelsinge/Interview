package strategy.good;

/**
 * Context class in the Strategy pattern.
 *
 * CheckoutService has NO knowledge of CreditCard, UPI, or Wallet.
 * It only knows the PaymentStrategy interface.
 *
 * Adding a new payment method = add a new Strategy class.
 * CheckoutService is never opened again.
 *
 * OCP: Open for extension (new strategies), Closed for modification.
 * DIP: Depends on PaymentStrategy abstraction, not concrete classes.
 */
public class CheckoutService {

    private PaymentStrategy paymentStrategy;

    // Strategy injected via constructor — DIP + testable (can inject mock)
    public CheckoutService(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    // Strategy can also be changed at runtime (e.g., user switches payment method)
    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    public void checkout(double amount) {
        System.out.println("Processing order for ₹" + amount);
        System.out.println("Using: " + paymentStrategy.getPaymentMethodName());

        // Validate limit before paying
        if (amount > paymentStrategy.getTransactionLimit()) {
            throw new IllegalArgumentException(
                "Amount ₹" + amount + " exceeds limit for " +
                paymentStrategy.getPaymentMethodName() +
                " (max: ₹" + paymentStrategy.getTransactionLimit() + ")"
            );
        }

        paymentStrategy.pay(amount);
        System.out.println("Order complete.\n");
    }
}
