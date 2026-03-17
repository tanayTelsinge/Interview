package factory.good;

public class SMSNotification implements Notification {

    @Override
    public void send(String recipient, String message) {
        System.out.println("[SMS -> " + recipient + "] " + message);
        System.out.println("  (via Twilio/MSG91, max 160 chars)");
    }

    @Override
    public String getChannel() {
        return "SMS";
    }
}
