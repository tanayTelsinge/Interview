package code.SRP.good;

/**
 * GOOD — SRP applied.
 * PaymentService has ONE reason to change: payment logic changes.
 */
public class PaymentService {

    public double calculateFee(String seatId) {
        return seatId.startsWith("VIP") ? 500.0 : 200.0;
    }

    public String chargeCard(String userId, double amount) {
        System.out.println("Charging " + amount + " to user " + userId);
        return "TXN" + userId + (long) amount;
    }

    public double calculateRefund(String bookingId) {
        // Refund policy logic isolated here
        return 150.0;
    }

    public void processRefund(String bookingId, double amount) {
        System.out.println("Refunding " + amount + " for booking " + bookingId);
    }
}
