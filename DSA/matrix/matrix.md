Spiral Matrix I (read order)
Type = inward
Emit = append(matrix[r][c])
Stop when visited all cells

Spiral Matrix II (generate 1..n²)
Type = inward
Emit = matrix[r][c] = counter++
Stop when counter > n²

Spiral Matrix III (walk outward)
Type = outward
Emit = record (r,c) if inside grid
Stop when count == rows×cols

Spiral Matrix IV (fill from linked list)
Type = inward
Emit = if list has value → place; else → place -1
Stop when filled all cells

```java
class SpiralEngine {
    static final int[][] DIRS = {{0,1},{1,0},{0,-1},{-1,0}}; // → ↓ ← ↑

    // Inward spiral (Matrix I, II, IV)
    public static void inwardSpiral(int rows, int cols, CellAction emit) {
        int top=0, bottom=rows-1, left=0, right=cols-1;
        while (top <= bottom && left <= right) {
            for (int c=left; c<=right; c++) emit.doAction(top, c);
            top++;
            for (int r=top; r<=bottom; r++) emit.doAction(r, right);
            right--;
            if (top<=bottom) {
                for (int c=right; c>=left; c--) emit.doAction(bottom, c);
                bottom--;
            }
            if (left<=right) {
                for (int r=bottom; r>=top; r--) emit.doAction(r, left);
                left++;
            }
        }
    }

    // Outward spiral (Matrix III)
    public static void outwardSpiral(int rows, int cols, int r, int c, CellAction emit) {
        int total = rows*cols, count=0;
        emit.doAction(r, c); count++;
        int stepLen=1, dirIndex=0;
        while (count < total) {
            for (int i=0; i<2; i++) {
                for (int k=0; k<stepLen; k++) {
                    r += DIRS[dirIndex][0];
                    c += DIRS[dirIndex][1];
                    if (r>=0 && r<rows && c>=0 && c<cols) {
                        emit.doAction(r, c); count++;
                        if (count==total) return;
                    }
                }
                dirIndex = (dirIndex+1)%4;
            }
            stepLen++;
        }
    }

    // Functional interface for what to do at each cell
    interface CellAction {
        void doAction(int r, int c);
    }
}

```