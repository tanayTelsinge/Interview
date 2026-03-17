package code.SRP.good;

/**
 * GOOD — SRP applied.
 *
 * BookingService has ONE reason to change: booking orchestration logic changes.
 * Payment, notification, and reporting concerns are delegated.
 *
 * If fee calculation changes → only PaymentService changes.
 * If email template changes  → only NotificationService changes.
 * BookingService stays stable.
 */
public class BookingService {

    private final PaymentService paymentService;
    private final NotificationService notificationService;

    // Dependencies injected — BookingService doesn't create them (DIP also applied)
    public BookingService(PaymentService paymentService, NotificationService notificationService) {
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    public String bookSeat(String userId, String movieId, String seatId) {
        System.out.println("Reserving seat " + seatId + " for user " + userId);
        String bookingId = "BK" + System.currentTimeMillis();

        // Delegates to PaymentService — no payment logic here
        double fee = paymentService.calculateFee(seatId);
        paymentService.chargeCard(userId, fee);

        // Delegates to NotificationService — no email logic here
        notificationService.sendBookingConfirmation(userId, bookingId, movieId, seatId);

        return bookingId;
    }

    public void cancelBooking(String userId, String bookingId) {
        System.out.println("Cancelling booking " + bookingId);

        double refund = paymentService.calculateRefund(bookingId);
        paymentService.processRefund(bookingId, refund);

        notificationService.sendCancellationNotice(userId, bookingId);
    }
}

class SRPDemo {
    public static void main(String[] args) {
        PaymentService payment = new PaymentService();
        NotificationService notification = new NotificationService();
        BookingService booking = new BookingService(payment, notification);

        String bookingId = booking.bookSeat("user123", "INTERSTELLAR", "VIP-A1");
        System.out.println("Booked: " + bookingId);

        System.out.println();
        booking.cancelBooking("user123", bookingId);
    }
}
