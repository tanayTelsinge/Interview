package Day4_problems.book_my_show.code.domain;

import java.util.List;

public class Screen {

    private String screenId;
    private int screenNumber;
    private List<Seat> seats;

    public Screen(String screenId, int screenNumber, List<Seat> seats) {
        this.screenId = screenId;
        this.screenNumber = screenNumber;
        this.seats = seats;
    }

    public String getScreenId()    { return screenId; }
    public int getScreenNumber()   { return screenNumber; }
    public List<Seat> getSeats()   { return seats; }
}
