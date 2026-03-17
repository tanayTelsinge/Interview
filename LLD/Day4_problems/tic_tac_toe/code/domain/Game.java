package Day4_problems.tic_tac_toe.code.domain;

import java.util.List;
import java.util.Scanner;

import Day4_problems.tic_tac_toe.code.enums.GameStatus;
import Day4_problems.tic_tac_toe.code.strategies.WinStrategy;

public class Game {

    private Board board;
    private List<Player> players;
    private int currentPlayerIndex;
    private WinStrategy winStrategy;
    private GameStatus gameStatus;

    public Game(Board board, List<Player> players, WinStrategy winStrategy) {
        this.board = board;
        this.players = players;
        this.winStrategy = winStrategy;
        this.currentPlayerIndex = 0;
        this.gameStatus = GameStatus.IN_PROGRESS;
    }

    public void play() {
        Scanner scanner = new Scanner(System.in);

        while (gameStatus == GameStatus.IN_PROGRESS) {
            board.printBoard();
            Player current = players.get(currentPlayerIndex);
            System.out.print(current.getName() + "'s turn [" + current.getSymbol() + "]. Enter row col (0-indexed): ");

            int row = scanner.nextInt();
            int col = scanner.nextInt();

            boolean moved = board.makeMove(row, col, current.getSymbol());
            if (!moved) continue; // invalid move — same player retries

            if (winStrategy.checkWin(board, row, col, current.getSymbol())) {
                board.printBoard();
                System.out.println(current.getName() + " wins!");
                gameStatus = GameStatus.WIN;
            } else if (board.isFull()) {
                board.printBoard();
                System.out.println("It's a draw!");
                gameStatus = GameStatus.DRAW;
            } else {
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            }
        }

        scanner.close();
    }
}
