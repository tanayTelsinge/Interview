package Day4_problems.snake_and_ladder.code;

import java.util.List;

import Day4_problems.snake_and_ladder.code.domain.Board;
import Day4_problems.snake_and_ladder.code.domain.Game;
import Day4_problems.snake_and_ladder.code.domain.Ladder;
import Day4_problems.snake_and_ladder.code.domain.Player;
import Day4_problems.snake_and_ladder.code.domain.Snake;
import Day4_problems.snake_and_ladder.code.strategies.StandardDice;

public class Solution {

    public static void main(String[] args) {
        List<Snake> snakes = List.of(
            new Snake(99, 54),
            new Snake(70, 55),
            new Snake(52, 42),
            new Snake(25, 2)
        );

        List<Ladder> ladders = List.of(
            new Ladder(6,  25),
            new Ladder(11, 40),
            new Ladder(60, 85),
            new Ladder(46, 90)
        );

        Board board = new Board(100, snakes, ladders);

        Player player1 = new Player("Alice");
        Player player2 = new Player("Bob");

        Game game = new Game(board, List.of(player1, player2), new StandardDice());
        game.play();
    }
}
