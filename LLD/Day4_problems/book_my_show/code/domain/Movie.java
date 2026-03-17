package Day4_problems.book_my_show.code.domain;

public class Movie {

    private String movieId;
    private String title;
    private int durationMins;
    private String genre;

    public Movie(String movieId, String title, int durationMins, String genre) {
        this.movieId = movieId;
        this.title = title;
        this.durationMins = durationMins;
        this.genre = genre;
    }

    public String getMovieId()    { return movieId; }
    public String getTitle()      { return title; }
    public int getDurationMins()  { return durationMins; }
    public String getGenre()      { return genre; }
}
