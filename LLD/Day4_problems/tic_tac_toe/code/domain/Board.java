package Day4_problems.tic_tac_toe.code.domain;

public class Board {

    private int size;
    private Cell[][] grid;

    public Board(int size) {
        this.size = size;
        this.grid = new Cell[size][size];
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                grid[i][j] = new Cell(i, j);
    }

    public boolean makeMove(int row, int col, char symbol) {
        if (row < 0 || row >= size || col < 0 || col >= size) {
            System.out.println("Invalid move: out of bounds. Try again.");
            return false;
        }
        if (!grid[row][col].isEmpty()) {
            System.out.println("Cell already occupied. Try again.");
            return false;
        }
        grid[row][col].setSymbol(symbol);
        return true;
    }

    public boolean isFull() {
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                if (grid[i][j].isEmpty()) return false;
        return true;
    }

    public Cell[][] getGrid() {
        return grid;
    }

    public int getSize() {
        return size;
    }

    public void printBoard() {
        System.out.println();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                char sym = grid[i][j].getSymbol();
                System.out.print(" " + (sym == '\0' ? '.' : sym) + " ");
                if (j < size - 1) System.out.print("|");
            }
            System.out.println();
            if (i < size - 1) System.out.println("---+".repeat(size - 1) + "---");
        }
        System.out.println();
    }
}
