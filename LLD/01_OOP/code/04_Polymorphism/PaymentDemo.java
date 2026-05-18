package code._04_Polymorphism;

/**
 * Demonstrates POLYMORPHISM — same interface, different behaviour at runtime.
 *
 * This is the mechanism behind Strategy, Factory, and Observer patterns.
 * Without polymorphism, you end up with if/else chains that violate OCP.
 *
 * Shows both:
 *  1. Runtime polymorphism (method overriding) — the important one
 *  2. Compile-time polymorphism (method overloading)
 */

// -------------------------------------------------------------------------
// RUNTIME POLYMORPHISM — same method call, different behaviour
// -------------------------------------------------------------------------

interface PaymentMethod {
    PaymentResult process(double amount);
    String getMethodName();
}

class PaymentResult {
    private final boolean success;
    private final String transactionId;
    private final String message;

    public PaymentResult(boolean success, String transactionId, String message) {
        this.success = success;
        this.transactionId = transactionId;
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("[%s] txnId=%s msg=%s", success ? "SUCCESS" : "FAILED", transactionId, message);
    }
}

class CreditCardPayment implements PaymentMethod {
    private final String cardNumber;

    public CreditCardPayment(String cardNumber) { this.cardNumber = cardNumber; }

    @Override
    public PaymentResult process(double amount) {
        System.out.println("Processing credit card payment of " + amount);
        // card-specific logic here
        return new PaymentResult(true, "CC-" + System.currentTimeMillis(), "Card charged successfully");
    }

    @Override
    public String getMethodName() { return "Credit Card"; }
}

class UPIPayment implements PaymentMethod {
    private final String upiId;

    public UPIPayment(String upiId) { this.upiId = upiId; }

    @Override
    public PaymentResult process(double amount) {
        System.out.println("Sending UPI request to " + upiId + " for amount " + amount);
        // UPI-specific logic here
        return new PaymentResult(true, "UPI-" + System.currentTimeMillis(), "UPI transfer complete");
    }

    @Override
    public String getMethodName() { return "UPI"; }
}

class WalletPayment implements PaymentMethod {
    private double walletBalance;

    public WalletPayment(double walletBalance) { this.walletBalance = walletBalance; }

    @Override
    public PaymentResult process(double amount) {
        if (walletBalance < amount) {
            return new PaymentResult(false, null, "Insufficient wallet balance");
        }
        walletBalance -= amount;
        System.out.println("Wallet debited by " + amount + ". Remaining: " + walletBalance);
        return new PaymentResult(true, "WAL-" + System.currentTimeMillis(), "Wallet payment successful");
    }

    @Override
    public String getMethodName() { return "Wallet"; }
}

// -------------------------------------------------------------------------
// CHECKOUT SERVICE — works with the interface, zero knowledge of concrete types
// -------------------------------------------------------------------------
class CheckoutService {

    // BAD — if/else chain, adding NetBanking means modifying this method (OCP violation)
    public void processBad(String type, double amount) {
        if (type.equals("CARD")) {
            System.out.println("Processing card...");
        } else if (type.equals("UPI")) {
            System.out.println("Processing UPI...");
        } else if (type.equals("WALLET")) {
            System.out.println("Processing wallet...");
        }
        // adding NetBanking = modify this method = risky
    }

    // GOOD — polymorphism: adding NetBanking = new class, zero changes here
    public PaymentResult processGood(PaymentMethod method, double amount) {
        System.out.println("Initiating payment via " + method.getMethodName());
        PaymentResult result = method.process(amount);
        System.out.println("Result: " + result);
        return result;
    }
}

// -------------------------------------------------------------------------
// COMPILE-TIME POLYMORPHISM — method overloading (less critical in interviews)
// -------------------------------------------------------------------------
class Logger {
    public void log(String message) {
        System.out.println("[INFO] " + message);
    }

    public void log(String message, Exception e) {       // overload — same name, different params
        System.out.println("[ERROR] " + message + ": " + e.getMessage());
    }

    public void log(String message, String level) {      // another overload
        System.out.println("[" + level + "] " + message);
    }
}

class PaymentDemo {
    public static void main(String[] args) {
        CheckoutService checkout = new CheckoutService();

        // Same method call, different behaviour — runtime polymorphism in action
        checkout.processGood(new CreditCardPayment("4111-1111-1111-1111"), 999.0);
        System.out.println();
        checkout.processGood(new UPIPayment("user@upi"), 499.0);
        System.out.println();
        checkout.processGood(new WalletPayment(1000.0), 750.0);
        System.out.println();

        // Adding NetBankingPayment tomorrow = new class only, CheckoutService unchanged
    }
}
