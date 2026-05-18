package factory.bad;

/**
 * VIOLATION: No factory — client code instantiates concrete classes directly.
 *
 * Problem: The client (OrderService) depends on concrete EmailNotification,
 * SMSNotification, and PushNotification classes. Adding WhatsApp notification
 * requires opening every class that creates notifications.
 *
 * DIP violation: high-level module depends on low-level concrete classes.
 */
public class NotificationFactory_Bad {

    // BAD: Client code knows about every concrete notification type
    public static void sendOrderConfirmation(String channel, String orderId) {
        if (channel.equals("EMAIL")) {
            // Client knows about EmailNotification internals
            System.out.println("Creating EmailNotification for order: " + orderId);
            System.out.println("[EMAIL] Order " + orderId + " confirmed.");

        } else if (channel.equals("SMS")) {
            System.out.println("Creating SMSNotification for order: " + orderId);
            System.out.println("[SMS] Order " + orderId + " confirmed.");

        } else if (channel.equals("PUSH")) {
            System.out.println("Creating PushNotification for order: " + orderId);
            System.out.println("[PUSH] Order " + orderId + " confirmed.");

        }
        // Adding WHATSAPP means opening this class — every class that creates
        // notifications needs similar changes = shotgun surgery
    }
}
