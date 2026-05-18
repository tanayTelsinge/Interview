package Day4_problems.snake_and_ladder.code.domain;

public class Player {

    private String name;
    private int position; // starts at 0 (off-board)

    public Player(String name) {
        this.name = name;
        this.position = 0;
    }

    public String getName()          { return name; }
    public int getPosition()         { return position; }
    public void setPosition(int pos) { this.position = pos; }
}
