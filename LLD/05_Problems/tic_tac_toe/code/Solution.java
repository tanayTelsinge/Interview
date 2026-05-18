package Day4_problems.tic_tac_toe.code;

import java.util.List;

import Day4_problems.tic_tac_toe.code.domain.Board;
import Day4_problems.tic_tac_toe.code.domain.Game;
import Day4_problems.tic_tac_toe.code.domain.Player;
import Day4_problems.tic_tac_toe.code.strategies.StandardWinStrategy;

public class Solution {

    public static void main(String[] args) {
        Board board = new Board(3);
        Player player1 = new Player("Alice", 'X');
        Player player2 = new Player("Bob", 'O');

        Game game = new Game(board, List.of(player1, player2), new StandardWinStrategy());
        game.play();
    }
}
