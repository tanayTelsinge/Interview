package strategy.good;

/**
 * Concrete Strategy: Credit Card payment.
 * Encapsulates all credit card–specific logic (fees, limits, authorization).
 *
 * Note: Strategies should be stateless when possible — all data flows in via pay().
 * If card details are needed, inject them via constructor (not stored as mutable state).
 */
public class CreditCardStrategy implements PaymentStrategy {

    private final String cardNumber;      // last 4 digits for display
    private final String cardHolderName;

    public CreditCardStrategy(String cardNumber, String cardHolderName) {
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
    }

    @Override
    public void pay(double amount) {
        double fee = amount * 0.02;   // 2% processing fee
        double total = amount + fee;
        System.out.println("=== Credit Card Payment ===");
        System.out.println("Card: **** **** **** " + cardNumber);
        System.out.println("Cardholder: " + cardHolderName);
        System.out.println("Amount: ₹" + amount);
        System.out.println("Processing fee (2%): ₹" + fee);
        System.out.println("Total charged: ₹" + total);
        System.out.println("Initiating card network authorization...");
        System.out.println("Payment successful!");
    }

    @Override
    public String getPaymentMethodName() {
        return "Credit Card (**** " + cardNumber + ")";
    }

    @Override
    public double getTransactionLimit() {
        return 100000.0;  // ₹1L per transaction
    }
}
