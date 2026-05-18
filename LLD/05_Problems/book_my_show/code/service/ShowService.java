package Day4_problems.book_my_show.code.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import Day4_problems.book_my_show.code.domain.Show;
import Day4_problems.book_my_show.code.domain.Theatre;

public class ShowService {

    private final List<Theatre> theatres = new ArrayList<>();
    private final List<Show> shows = new ArrayList<>();

    public void addTheatre(Theatre theatre) {
        theatres.add(theatre);
    }

    public void addShow(Show show) {
        shows.add(show);
    }

    public List<Show> searchShows(String movieTitle, String city) {
        return shows.stream()
                .filter(show -> show.getMovie().getTitle().equalsIgnoreCase(movieTitle)
                        && show.getTheatre().getCity().equalsIgnoreCase(city))
                .collect(Collectors.toList());
    }
}
