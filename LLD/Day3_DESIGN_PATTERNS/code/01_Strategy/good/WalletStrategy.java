package strategy.good;

/**
 * Concrete Strategy: Wallet payment (e.g., Paytm, PhonePe wallet).
 *
 * This was added AFTER CheckoutService was written.
 * CheckoutService.java was NOT touched to accommodate this — that's OCP.
 */
public class WalletStrategy implements PaymentStrategy {

    private final String walletProvider;
    private double balance;

    public WalletStrategy(String walletProvider, double balance) {
        this.walletProvider = walletProvider;
        this.balance = balance;
    }

    @Override
    public void pay(double amount) {
        if (balance < amount) {
            throw new IllegalStateException("Insufficient wallet balance. Available: ₹" + balance);
        }
        balance -= amount;
        System.out.println("=== Wallet Payment ===");
        System.out.println("Provider: " + walletProvider);
        System.out.println("Amount deducted: ₹" + amount);
        System.out.println("Remaining balance: ₹" + balance);
        System.out.println("Payment successful!");
    }

    @Override
    public String getPaymentMethodName() {
        return walletProvider + " Wallet";
    }

    @Override
    public double getTransactionLimit() {
        return 10000.0;  // ₹10K wallet limit
    }
}
