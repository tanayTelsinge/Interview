package code.SRP.good;

/**
 * GOOD — SRP applied.
 * NotificationService has ONE reason to change: notification/email logic changes.
 */
public class NotificationService {

    public void sendBookingConfirmation(String userId, String bookingId, String movieId, String seatId) {
        String email = resolveEmail(userId);
        System.out.println("Sending booking confirmation to " + email
                + " [booking=" + bookingId + ", movie=" + movieId + ", seat=" + seatId + "]");
        // HTML template, SMTP logic isolated here
    }

    public void sendCancellationNotice(String userId, String bookingId) {
        String email = resolveEmail(userId);
        System.out.println("Sending cancellation notice to " + email + " for booking " + bookingId);
    }

    private String resolveEmail(String userId) {
        return userId + "@example.com";
    }
}
