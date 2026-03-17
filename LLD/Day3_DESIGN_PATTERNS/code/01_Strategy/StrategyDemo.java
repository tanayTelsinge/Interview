package strategy;

import strategy.good.*;

/**
 * Demo: Strategy Pattern
 *
 * Run this to see the pattern in action.
 * Shows runtime strategy swapping — the same CheckoutService
 * with three different payment behaviors.
 */
public class StrategyDemo {

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("   STRATEGY PATTERN DEMO");
        System.out.println("========================================\n");

        // --- Credit Card ---
        PaymentStrategy card = new CreditCardStrategy("4242", "Rahul Sharma");
        CheckoutService checkout = new CheckoutService(card);
        checkout.checkout(5000);

        // --- Switch to UPI at runtime (no code change in CheckoutService) ---
        PaymentStrategy upi = new UPIStrategy("rahul@upi");
        checkout.setPaymentStrategy(upi);
        checkout.checkout(15000);

        // --- Switch to Wallet ---
        PaymentStrategy wallet = new WalletStrategy("Paytm", 3000);
        checkout.setPaymentStrategy(wallet);
        checkout.checkout(2500);

        // --- Limit violation ---
        System.out.println("--- Testing limit violation ---");
        try {
            PaymentStrategy walletSmall = new WalletStrategy("Paytm", 500);
            checkout.setPaymentStrategy(walletSmall);
            checkout.checkout(15000);  // Exceeds ₹10K wallet limit
        } catch (IllegalArgumentException e) {
            System.out.println("Caught expected error: " + e.getMessage());
        }

        System.out.println("\n--- Interview takeaway ---");
        System.out.println("CheckoutService was NEVER modified to add UPI or Wallet.");
        System.out.println("That's OCP. Strategy injected via constructor = DIP.");
    }
}
