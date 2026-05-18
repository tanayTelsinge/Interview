package Day4_problems.book_my_show.code;

import java.time.LocalDateTime;
import java.util.List;

import Day4_problems.book_my_show.code.domain.Booking;
import Day4_problems.book_my_show.code.domain.Movie;
import Day4_problems.book_my_show.code.domain.Screen;
import Day4_problems.book_my_show.code.domain.Seat;
import Day4_problems.book_my_show.code.domain.Show;
import Day4_problems.book_my_show.code.domain.Theatre;
import Day4_problems.book_my_show.code.domain.User;
import Day4_problems.book_my_show.code.enums.SeatType;
import Day4_problems.book_my_show.code.service.BookingService;
import Day4_problems.book_my_show.code.service.PaymentService;
import Day4_problems.book_my_show.code.service.ShowService;
import Day4_problems.book_my_show.code.strategies.RandomSeatSelectionStrategy;
import Day4_problems.book_my_show.code.strategies.SeatSelectionStrategy;

public class Solution {

    public static void main(String[] args) {
        // --- Setup services ---
        ShowService showService       = new ShowService();
        BookingService bookingService = new BookingService(new PaymentService());
        SeatSelectionStrategy seatStrategy = new RandomSeatSelectionStrategy();

        // --- Create movie ---
        Movie movie = new Movie("M1", "Interstellar", 169, "Sci-Fi");

        // --- Create seats for Screen 1 ---
        List<Seat> seats = List.of(
            new Seat("S1", SeatType.SILVER,   1, 1),
            new Seat("S2", SeatType.SILVER,   1, 2),
            new Seat("S3", SeatType.SILVER,   1, 3),
            new Seat("G1", SeatType.GOLD,     2, 1),
            new Seat("G2", SeatType.GOLD,     2, 2),
            new Seat("P1", SeatType.PLATINUM, 3, 1),
            new Seat("P2", SeatType.PLATINUM, 3, 2)
        );

        // --- Create theatre with one screen ---
        Screen screen   = new Screen("SC1", 1, seats);
        Theatre theatre = new Theatre("T1", "PVR Cinemas", "Bangalore", List.of(screen));
        showService.addTheatre(theatre);

        // --- Create show ---
        Show show = new Show("SH1", movie, screen, theatre, LocalDateTime.of(2026, 3, 17, 18, 0));
        showService.addShow(show);

        // --- Users ---
        User alice = new User("U1", "Alice", "alice@example.com");
        User bob   = new User("U2", "Bob",   "bob@example.com");

        // --- Search shows ---
        List<Show> results = showService.searchShows("Interstellar", "Bangalore");
        System.out.println("Shows found: " + results.size());
        Show selectedShow = results.get(0);
        System.out.println("Available seats: " + selectedShow.getAvailableSeats().size());

        // --- Alice books 2 GOLD seats ---
        System.out.println("\n--- Alice booking 2 GOLD seats ---");
        List<String> aliceSeats = seatStrategy.selectSeats(selectedShow, 2, SeatType.GOLD);
        Booking aliceBooking = bookingService.bookSeats(alice, selectedShow, aliceSeats);
        System.out.println(aliceBooking);

        // --- Bob books 1 SILVER seat ---
        System.out.println("\n--- Bob booking 1 SILVER seat ---");
        System.out.println("Available seats now: " + selectedShow.getAvailableSeats().size());
        List<String> bobSeats = seatStrategy.selectSeats(selectedShow, 1, SeatType.SILVER);
        Booking bobBooking = bookingService.bookSeats(bob, selectedShow, bobSeats);
        System.out.println(bobBooking);

        // --- Alice cancels ---
        System.out.println("\n--- Alice cancels booking ---");
        bookingService.cancelBooking(aliceBooking);
        System.out.println("Available seats after cancel: " + selectedShow.getAvailableSeats().size());
    }
}
