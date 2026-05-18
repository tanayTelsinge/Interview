package factory;

import factory.good.*;

/**
 * Demo: Factory Pattern
 *
 * OrderService uses factory — it never knows which concrete Notification it gets.
 */
public class FactoryDemo {

    // High-level module: only depends on Notification interface
    static void sendOrderConfirmation(String channel, String orderId, String recipient) {
        Notification notification = NotificationFactory.create(channel);  // Factory does creation
        notification.send(recipient, "Your order " + orderId + " has been confirmed!");
    }

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("   FACTORY PATTERN DEMO");
        System.out.println("========================================\n");

        sendOrderConfirmation("EMAIL", "ORD-001", "user@example.com");
        System.out.println();
        sendOrderConfirmation("SMS", "ORD-002", "+91-9999999999");
        System.out.println();
        sendOrderConfirmation("PUSH", "ORD-003", "device-token-xyz");
        System.out.println();

        // Unknown channel
        try {
            sendOrderConfirmation("WHATSAPP", "ORD-004", "+91-8888888888");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        System.out.println("\n--- Interview takeaway ---");
        System.out.println("sendOrderConfirmation() never imports EmailNotification.");
        System.out.println("It only knows Notification interface. That's DIP via Factory.");
    }
}
