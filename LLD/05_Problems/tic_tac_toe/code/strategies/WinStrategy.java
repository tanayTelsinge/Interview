package Day4_problems.tic_tac_toe.code.strategies;

import Day4_problems.tic_tac_toe.code.domain.Board;

public interface WinStrategy {
    boolean checkWin(Board board, int lastRow, int lastCol, char symbol);
}
