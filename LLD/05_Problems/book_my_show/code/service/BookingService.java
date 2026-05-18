package Day4_problems.book_my_show.code.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Day4_problems.book_my_show.code.domain.Booking;
import Day4_problems.book_my_show.code.domain.Payment;
import Day4_problems.book_my_show.code.domain.Seat;
import Day4_problems.book_my_show.code.domain.Show;
import Day4_problems.book_my_show.code.domain.User;
import Day4_problems.book_my_show.code.enums.BookingStatus;
import Day4_problems.book_my_show.code.enums.PaymentStatus;

public class BookingService {

    private final PaymentService paymentService;

    public BookingService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public Booking bookSeats(User user, Show show, List<String> seatNumbers) {
        // Step 1: lock seats atomically — handles concurrent bookings
        boolean locked = show.lockSeats(seatNumbers);
        if (!locked) {
            throw new RuntimeException("One or more seats are no longer available: " + seatNumbers);
        }

        String bookingId = "BKG_" + System.currentTimeMillis();
        double totalAmount = calculateAmount(show, seatNumbers);

        // Step 2: process payment
        Payment payment = paymentService.processPayment(bookingId, totalAmount);

        if (PaymentStatus.SUCCESS.equals(payment.getStatus())) {
            show.confirmSeats(seatNumbers);
            return new Booking(bookingId, user, show, seatNumbers, totalAmount, BookingStatus.CONFIRMED);
        } else {
            // payment failed — release the lock so other users can book
            show.releaseSeats(seatNumbers);
            return new Booking(bookingId, user, show, seatNumbers, totalAmount, BookingStatus.PAYMENT_FAILED);
        }
    }

    public void cancelBooking(Booking booking) {
        booking.getShow().releaseSeats(booking.getSeatNumbers());
        booking.setStatus(BookingStatus.CANCELLED);
        System.out.println("Booking " + booking.getBookingId() + " cancelled. Seats released.");
    }

    private double calculateAmount(Show show, List<String> seatNumbers) {
        Map<String, Seat> seatMap = new HashMap<>();
        show.getScreen().getSeats().forEach(seat -> seatMap.put(seat.getSeatNumber(), seat));
        return seatNumbers.stream()
                .mapToDouble(sn -> seatMap.get(sn).getSeatType().getPrice())
                .sum();
    }
}
