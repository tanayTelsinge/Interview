package Day4_problems.snake_and_ladder.code.domain;

import java.util.List;

import Day4_problems.snake_and_ladder.code.strategies.DiceStrategy;

public class Game {

    private Board board;
    private List<Player> players;
    private DiceStrategy dice;
    private int currentPlayerIndex;
    private boolean gameOver;

    public Game(Board board, List<Player> players, DiceStrategy dice) {
        this.board = board;
        this.players = players;
        this.dice = dice;
        this.currentPlayerIndex = 0;
        this.gameOver = false;
    }

    public void play() {
        while (!gameOver) {
            Player current = players.get(currentPlayerIndex);
            int roll = dice.roll();
            int oldPosition = current.getPosition();
            int newPosition = oldPosition + roll;

            if (newPosition > board.getSize()) {
                System.out.println(current.getName() + " rolled " + roll
                        + " → overshoot, stays at " + oldPosition);
            } else {
                int resolvedPosition = board.resolvePosition(newPosition);

                String effect = "";
                if (resolvedPosition < newPosition)
                    effect = "  [SNAKE: " + newPosition + " ↓ " + resolvedPosition + "]";
                else if (resolvedPosition > newPosition)
                    effect = "  [LADDER: " + newPosition + " ↑ " + resolvedPosition + "]";

                current.setPosition(resolvedPosition);
                System.out.println(current.getName() + " rolled " + roll
                        + ": " + oldPosition + " → " + resolvedPosition + effect);

                if (resolvedPosition == board.getSize()) {
                    System.out.println("\n" + current.getName() + " wins!");
                    gameOver = true;
                    return;
                }
            }

            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }
    }
}
