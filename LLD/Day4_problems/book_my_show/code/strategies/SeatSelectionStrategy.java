package Day4_problems.book_my_show.code.strategies;

import java.util.List;

import Day4_problems.book_my_show.code.domain.Show;
import Day4_problems.book_my_show.code.enums.SeatType;

public interface SeatSelectionStrategy {
    List<String> selectSeats(Show show, int count, SeatType seatType);
}
