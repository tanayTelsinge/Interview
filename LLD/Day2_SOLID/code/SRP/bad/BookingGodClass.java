package code.SRP.bad;

/**
 * BAD EXAMPLE — SRP Violated.
 *
 * BookingGodClass has FOUR reasons to change:
 *  1. Booking logic changes    → modify this class
 *  2. Payment logic changes    → modify this class
 *  3. Email template changes   → modify this class
 *  4. Report format changes    → modify this class
 *
 * Interviewer's probe: "What if we change how fees are calculated?"
 * Answer: "Modify BookingGodClass" → SRP violated → No-hire signal.
 */
public class BookingGodClass {

    // Handles booking
    public String bookSeat(String userId, String movieId, String seatId) {
        System.out.println("Checking seat availability for " + seatId);
        System.out.println("Reserving seat " + seatId + " for user " + userId);
        String bookingId = "BK" + System.currentTimeMillis();

        // Handles payment — payment responsibility mixed in
        double amount = calculateFee(seatId);
        String txnId = chargeCard(userId, amount);
        System.out.println("Charged " + amount + ", txn: " + txnId);

        // Handles notification — notification responsibility mixed in
        String email = getUserEmail(userId);
        sendBookingConfirmationEmail(email, bookingId, movieId, seatId);

        // Handles reporting — reporting responsibility mixed in
        saveBookingReport(bookingId, userId, movieId, seatId, amount);

        return bookingId;
    }

    // Booking concern
    public void cancelBooking(String bookingId) {
        System.out.println("Cancelling booking " + bookingId);
        double refund = calculateRefund(bookingId);

        // Payment concern mixed in again
        processRefund(bookingId, refund);

        // Notification concern mixed in again
        sendCancellationEmail(bookingId);
    }

    // --- Payment logic (should be in PaymentService) ---
    private double calculateFee(String seatId) {
        return seatId.startsWith("VIP") ? 500.0 : 200.0;
    }

    private String chargeCard(String userId, double amount) {
        return "TXN" + userId + amount;
    }

    private double calculateRefund(String bookingId) {
        return 150.0; // simplified
    }

    private void processRefund(String bookingId, double amount) {
        System.out.println("Refunding " + amount + " for booking " + bookingId);
    }

    // --- Notification logic (should be in NotificationService) ---
    private String getUserEmail(String userId) {
        return userId + "@example.com";
    }

    private void sendBookingConfirmationEmail(String email, String bookingId, String movie, String seat) {
        System.out.println("Sending booking confirmation to " + email);
        // HTML email template logic here...
    }

    private void sendCancellationEmail(String bookingId) {
        System.out.println("Sending cancellation email for " + bookingId);
    }

    // --- Reporting logic (should be in ReportService) ---
    private void saveBookingReport(String bookingId, String userId, String movieId, String seatId, double amount) {
        System.out.println("Saving report for booking " + bookingId);
        // CSV/DB report logic here...
    }
}
