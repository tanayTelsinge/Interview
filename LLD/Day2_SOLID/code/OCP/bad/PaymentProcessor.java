package Day2_SOLID.code.OCP.bad;

/**
 * BAD EXAMPLE — OCP Violated.
 *
 * Every time a new payment method is added (NetBanking, PayTM, Crypto),
 * we MODIFY this existing class — risk of breaking existing UPI/Card logic.
 *
 * Interviewer's probe: "Now add PayTM support."
 * Answer: "Add else-if to processPayment()" → OCP violated → No-hire signal.
 */
public class PaymentProcessor {

    public void processPayment(String type, double amount) {
        if (type.equals("CREDIT_CARD")) {
            System.out.println("Processing credit card payment of " + amount);
            // card-specific logic: network call, CVV check, etc.

        } else if (type.equals("UPI")) {
            System.out.println("Processing UPI payment of " + amount);
            // UPI-specific logic: VPA validation, UPI PIN, etc.

        } else if (type.equals("WALLET")) {
            System.out.println("Processing wallet payment of " + amount);
            // wallet-specific logic: balance check, debit, etc.

        }
        // Adding PayTM = add another else-if here = modifying existing code = risky
        // What if you accidentally break the UPI branch?
    }

    public double calculateFee(String type, double amount) {
        if (type.equals("CREDIT_CARD")) {
            return amount * 0.02;       // 2% fee
        } else if (type.equals("UPI")) {
            return 0;                    // free
        } else if (type.equals("WALLET")) {
            return amount * 0.01;       // 1% fee
        }
        return 0;
        // Again — adding PayTM means modifying this method too
    }
}
