package factory.good;

public class EmailNotification implements Notification {

    @Override
    public void send(String recipient, String message) {
        System.out.println("[EMAIL -> " + recipient + "] " + message);
        System.out.println("  (via SMTP gateway, formatted as HTML)");
    }

    @Override
    public String getChannel() {
        return "EMAIL";
    }
}
