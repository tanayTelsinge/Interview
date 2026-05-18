package Day4_problems.book_my_show.code.domain;

import Day4_problems.book_my_show.code.enums.SeatType;

public class Seat {

    private String seatNumber;
    private SeatType seatType;
    private int row;
    private int col;

    public Seat(String seatNumber, SeatType seatType, int row, int col) {
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.row = row;
        this.col = col;
    }

    public String getSeatNumber() { return seatNumber; }
    public SeatType getSeatType() { return seatType; }
    public int getRow()           { return row; }
    public int getCol()           { return col; }
}
