package strategy.good;

/**
 * Strategy interface — the abstraction that CheckoutService depends on.
 *
 * Each payment method is a concrete implementation of this interface.
 * CheckoutService never needs to know which implementation it's using.
 *
 * DIP: High-level module (CheckoutService) depends on this abstraction,
 *      not on concrete CreditCardStrategy or UPIStrategy.
 */
public interface PaymentStrategy {

    /**
     * Execute the payment for the given amount.
     * Implementations handle their own fee calculation and processing logic.
     */
    void pay(double amount);

    /**
     * Human-readable name for logging, receipts, and display.
     */
    String getPaymentMethodName();

    /**
     * Maximum transaction limit for this payment method.
     * Default: no limit (can be overridden by implementations).
     */
    default double getTransactionLimit() {
        return Double.MAX_VALUE;
    }
}
