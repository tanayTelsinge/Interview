package strategy.bad;

/**
 * VIOLATION: Strategy pattern NOT applied.
 *
 * Problem: PaymentProcessor knows about every payment type.
 * Adding "NET_BANKING" means opening this class and adding another else-if.
 * This violates OCP — every new payment method breaks the closed-for-modification rule.
 *
 * Interview red flag: Cascading if/else on a type string is a signal that
 * Strategy pattern is being missed.
 */
public class PaymentProcessor_Bad {

    // BAD: type string controls behavior — this is a Strategy pattern smell
    public void processPayment(String type, double amount) {
        if (type.equals("CREDIT_CARD")) {
            double fee = amount * 0.02;   // 2% processing fee
            System.out.println("Charging credit card. Amount: " + amount + ", Fee: " + fee);
            System.out.println("Initiating card network authorization...");
            System.out.println("Total charged: " + (amount + fee));

        } else if (type.equals("UPI")) {
            System.out.println("Initiating UPI transfer. Amount: " + amount);
            System.out.println("UPI Reference: UPI" + System.currentTimeMillis());

        } else if (type.equals("WALLET")) {
            System.out.println("Deducting from wallet. Amount: " + amount);
            System.out.println("Wallet balance updated.");

        } else if (type.equals("NET_BANKING")) {
            // Adding this required opening and modifying this class = OCP violation
            double fee = amount * 0.01;
            System.out.println("Net banking transfer. Amount: " + amount + ", Fee: " + fee);

        } else {
            throw new IllegalArgumentException("Unknown payment type: " + type);
        }
    }

    // BAD: Validation also mixed in here — SRP violation on top of OCP violation
    public boolean validatePayment(String type, double amount) {
        if (type.equals("CREDIT_CARD")) {
            return amount <= 100000;  // 1L limit
        } else if (type.equals("UPI")) {
            return amount <= 200000;  // 2L limit
        } else if (type.equals("WALLET")) {
            return amount <= 10000;   // 10K limit
        }
        return true;
    }

    // Interview question: "How would you add EMI as a payment option?"
    // Answer with this code: "I have to open and modify PaymentProcessor_Bad — that's the problem."
}
