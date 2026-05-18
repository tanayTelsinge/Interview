package factory.good;

/**
 * Simple Factory — centralizes object creation logic.
 *
 * This is NOT a GoF pattern, but it's what 90% of interviews mean
 * when they say "Factory." Know the distinction if asked:
 *
 *   Simple Factory: one class, switch/if-else, static create() method
 *   Factory Method:  abstract creator class, subclasses override createProduct()
 *   Abstract Factory: families of related objects
 *
 * The switch here is intentional and correct — it's in ONE place.
 * Adding WhatsApp = add WhatsAppNotification class + one case here.
 * No other class is touched.
 *
 * Client code (OrderService) NEVER calls new EmailNotification() — it calls
 * NotificationFactory.create("EMAIL") and gets back a Notification interface.
 * DIP in action: client depends on the abstraction, not the concrete class.
 */
public class NotificationFactory {

    public static Notification create(String channel) {
        return switch (channel.toUpperCase()) {
            case "EMAIL" -> new EmailNotification();
            case "SMS"   -> new SMSNotification();
            case "PUSH"  -> new PushNotification();
            // Adding WHATSAPP: add WhatsAppNotification.java + one case here
            // OrderService.java is NOT touched
            default -> throw new IllegalArgumentException(
                "Unknown notification channel: " + channel +
                ". Supported: EMAIL, SMS, PUSH"
            );
        };
    }
}
