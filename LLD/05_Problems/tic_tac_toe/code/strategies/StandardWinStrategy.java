package Day4_problems.tic_tac_toe.code.strategies;

import Day4_problems.tic_tac_toe.code.domain.Board;
import Day4_problems.tic_tac_toe.code.domain.Cell;

public class StandardWinStrategy implements WinStrategy {

    @Override
    public boolean checkWin(Board board, int lastRow, int lastCol, char symbol) {
        int n = board.getSize();
        Cell[][] grid = board.getGrid();

        // Check row
        boolean win = true;
        for (int j = 0; j < n; j++)
            if (grid[lastRow][j].getSymbol() != symbol) { win = false; break; }
        if (win) return true;

        // Check column
        win = true;
        for (int i = 0; i < n; i++)
            if (grid[i][lastCol].getSymbol() != symbol) { win = false; break; }
        if (win) return true;

        // Check main diagonal (top-left to bottom-right) — only if last move is on it
        if (lastRow == lastCol) {
            win = true;
            for (int i = 0; i < n; i++)
                if (grid[i][i].getSymbol() != symbol) { win = false; break; }
            if (win) return true;
        }

        // Check anti-diagonal (top-right to bottom-left) — only if last move is on it
        if (lastRow + lastCol == n - 1) {
            win = true;
            for (int i = 0; i < n; i++)
                if (grid[i][n - 1 - i].getSymbol() != symbol) { win = false; break; }
            if (win) return true;
        }

        return false;
    }
}
