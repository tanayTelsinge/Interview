package Day4_problems.book_my_show.code.service;

import Day4_problems.book_my_show.code.domain.Payment;
import Day4_problems.book_my_show.code.enums.PaymentStatus;

public class PaymentService {

    public Payment processPayment(String bookingId, double amount) {
        // Simulated payment — always succeeds
        String paymentId = "PAY_" + System.currentTimeMillis();
        System.out.println("  Payment processed: Rs." + amount + " [" + paymentId + "]");
        return new Payment(paymentId, bookingId, amount, PaymentStatus.SUCCESS);
    }
}
