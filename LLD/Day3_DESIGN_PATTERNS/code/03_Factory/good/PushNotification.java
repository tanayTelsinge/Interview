package factory.good;

public class PushNotification implements Notification {

    @Override
    public void send(String recipient, String message) {
        System.out.println("[PUSH -> device:" + recipient + "] " + message);
        System.out.println("  (via FCM/APNs)");
    }

    @Override
    public String getChannel() {
        return "PUSH";
    }
}
