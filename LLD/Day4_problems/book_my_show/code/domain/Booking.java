package Day4_problems.book_my_show.code.domain;

import java.util.List;

import Day4_problems.book_my_show.code.enums.BookingStatus;

public class Booking {

    private String bookingId;
    private User user;
    private Show show;
    private List<String> seatNumbers;
    private double totalAmount;
    private BookingStatus status;

    public Booking(String bookingId, User user, Show show,
                   List<String> seatNumbers, double totalAmount, BookingStatus status) {
        this.bookingId = bookingId;
        this.user = user;
        this.show = show;
        this.seatNumbers = seatNumbers;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public String getBookingId()          { return bookingId; }
    public Show getShow()                 { return show; }
    public List<String> getSeatNumbers()  { return seatNumbers; }
    public double getTotalAmount()        { return totalAmount; }
    public BookingStatus getStatus()      { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "Booking[" + bookingId + "] " + user.getName()
                + " | " + show.getMovie().getTitle()
                + " | Seats: " + seatNumbers
                + " | Rs." + totalAmount
                + " | " + status;
    }
}
