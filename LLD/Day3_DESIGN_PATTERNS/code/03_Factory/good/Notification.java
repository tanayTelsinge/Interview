package factory.good;

/**
 * Product interface — all notification types implement this.
 *
 * The client (OrderService) only knows about Notification.
 * It doesn't care if it's Email, SMS, or Push under the hood.
 */
public interface Notification {

    void send(String recipient, String message);

    String getChannel();
}
