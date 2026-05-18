- Game orchestrates the loop: holds Board, List<Player>, WinStrategy, GameStatus.
- Board: N×N grid of Cells, makeMove(), isFull(), printBoard().
- Cell: row, col, char symbol ('\0' = empty). Avoids enum — supports any symbol.
- Player: name + char symbol.
- WinStrategy (interface): checkWin(board, lastRow, lastCol, symbol).
- StandardWinStrategy: checks row, col, and diagonals of the last move only.
- GameStatus enum: IN_PROGRESS, WIN, DRAW.


Flow:
Game starts → Board(N) created → Players added
  Loop (while IN_PROGRESS):
    → printBoard()
    → current player inputs (row, col)
    → board.makeMove() — validates bounds + occupied; returns false → retry same player
    → winStrategy.checkWin() using last move position (O(N), not O(N²))
    → board.isFull() → DRAW
    → advance player: (index + 1) % players.size()  ← supports 2+ players


Design Patterns used:

- Strategy - WinStrategy (StandardWinStrategy)
  - Swap win rules without touching Game (e.g., connect-K, gravity-based)


SOLID Principles:

- SRP  - each class has one job: Cell=data, Board=grid ops, Game=orchestration, WinStrategy=win logic
- OCP  - new win rules = new WinStrategy impl, Game unchanged
- LSP  - WinStrategy interface; StandardWinStrategy is substitutable for any future impl
- DIP  - Game depends on WinStrategy interface, not StandardWinStrategy directly
- ISP  - WinStrategy has single focused method; no bloated interface


Key decisions worth mentioning in interview:

1. Win check at O(N): only check row/col/diagonals of last move — no full board scan
   - Only check main diagonal if lastRow == lastCol
   - Only check anti-diagonal if lastRow + lastCol == N-1

2. Circular player rotation: (currentPlayerIndex + 1) % players.size()
   - Handles 2+ players with no extra logic

3. makeMove returns boolean — invalid move retries same player (loop continues without index advance)

4. char symbol in Cell (not enum) — supports any symbol: X, O, #, @ etc.


Known limitations / future scope:

- No AI player (design allows: create BotPlayer that overrides move selection)
- No game replay (wrap play() loop in outer Game.restart())
- No concurrency needed (single-threaded turn-based game)
