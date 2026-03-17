package Day4_problems.book_my_show.code.domain;

import Day4_problems.book_my_show.code.enums.PaymentStatus;

public class Payment {

    private String paymentId;
    private String bookingId;
    private double amount;
    private PaymentStatus status;

    public Payment(String paymentId, String bookingId, double amount, PaymentStatus status) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.amount = amount;
        this.status = status;
    }

    public String getPaymentId()     { return paymentId; }
    public String getBookingId()     { return bookingId; }
    public double getAmount()        { return amount; }
    public PaymentStatus getStatus() { return status; }
}
