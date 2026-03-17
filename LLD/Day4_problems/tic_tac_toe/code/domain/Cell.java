package Day4_problems.tic_tac_toe.code.domain;

public class Cell {

    private int row;
    private int col;
    private char symbol; // '\0' means empty

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.symbol = '\0';
    }

    public boolean isEmpty() {
        return symbol == '\0';
    }

    public char getSymbol() {
        return symbol;
    }

    public void setSymbol(char symbol) {
        this.symbol = symbol;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }
}
