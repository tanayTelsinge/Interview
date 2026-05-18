package Day4_problems.book_my_show.code.domain;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Day4_problems.book_my_show.code.enums.SeatStatus;

public class Show {

    private String showId;
    private Movie movie;
    private Screen screen;
    private Theatre theatre;
    private LocalDateTime startTime;
    private Map<String, SeatStatus> seatStatusMap;

    public Show(String showId, Movie movie, Screen screen, Theatre theatre, LocalDateTime startTime) {
        this.showId = showId;
        this.movie = movie;
        this.screen = screen;
        this.theatre = theatre;
        this.startTime = startTime;
        this.seatStatusMap = new HashMap<>();
        // initialise every seat in this screen as AVAILABLE for this show
        screen.getSeats().forEach(seat -> seatStatusMap.put(seat.getSeatNumber(), SeatStatus.AVAILABLE));
    }

    // synchronized: check-then-act must be atomic to prevent double booking
    public synchronized boolean lockSeats(List<String> seatNumbers) {
        for (String sn : seatNumbers) {
            if (!SeatStatus.AVAILABLE.equals(seatStatusMap.get(sn))) return false;
        }
        seatNumbers.forEach(sn -> seatStatusMap.put(sn, SeatStatus.LOCKED));
        return true;
    }

    public synchronized void confirmSeats(List<String> seatNumbers) {
        seatNumbers.forEach(sn -> seatStatusMap.put(sn, SeatStatus.BOOKED));
    }

    public synchronized void releaseSeats(List<String> seatNumbers) {
        seatNumbers.forEach(sn -> seatStatusMap.put(sn, SeatStatus.AVAILABLE));
    }

    public List<Seat> getAvailableSeats() {
        return screen.getSeats().stream()
                .filter(seat -> SeatStatus.AVAILABLE.equals(seatStatusMap.get(seat.getSeatNumber())))
                .collect(Collectors.toList());
    }

    public String getShowId()            { return showId; }
    public Movie getMovie()              { return movie; }
    public Screen getScreen()            { return screen; }
    public Theatre getTheatre()          { return theatre; }
    public LocalDateTime getStartTime()  { return startTime; }
    public Map<String, SeatStatus> getSeatStatusMap() { return seatStatusMap; }
}
