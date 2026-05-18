package Day4_problems.book_my_show.code.strategies;

import java.util.List;
import java.util.stream.Collectors;

import Day4_problems.book_my_show.code.domain.Seat;
import Day4_problems.book_my_show.code.domain.Show;
import Day4_problems.book_my_show.code.enums.SeatType;

public class RandomSeatSelectionStrategy implements SeatSelectionStrategy {

    @Override
    public List<String> selectSeats(Show show, int count, SeatType seatType) {
        List<String> selected = show.getAvailableSeats().stream()
                .filter(seat -> seat.getSeatType() == seatType)
                .limit(count)
                .map(Seat::getSeatNumber)
                .collect(Collectors.toList());

        if (selected.size() < count) {
            throw new RuntimeException("Not enough " + seatType + " seats available. Requested: "
                    + count + ", Available: " + selected.size());
        }
        return selected;
    }
}
