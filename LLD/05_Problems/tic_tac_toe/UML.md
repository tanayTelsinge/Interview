# Tic Tac Toe — UML Class Diagram

> Matches the actual implementation in 05_Problems/tic_tac_toe/code/

---

## Class Diagram

```
«enumeration»
GameStatus
──────────
IN_PROGRESS
WIN
DRAW


┌──────────────────────┐
│        Cell           │
├──────────────────────┤
│ - row: int            │
│ - col: int            │
│ - symbol: char        │  ('\0' = empty)
├──────────────────────┤
│ + isEmpty(): boolean  │
│ + getSymbol(): char   │
│ + setSymbol(char)     │
└──────────────────────┘


┌──────────────────────────────────┐
│            Board                  │
├──────────────────────────────────┤
│ - size: int                       │
│ - grid: Cell[][]             ◆   │  ← composition
├──────────────────────────────────┤
│ + makeMove(row, col, symbol)      │
│   : boolean                       │
│ + isFull(): boolean               │
│ + printBoard(): void              │
│ + getGrid(): Cell[][]             │
│ + getSize(): int                  │
└──────────────────────────────────┘


┌──────────────────────┐
│        Player         │
├──────────────────────┤
│ - name: String        │
│ - symbol: char        │
├──────────────────────┤
│ + getName(): String   │
│ + getSymbol(): char   │
└──────────────────────┘


┌────────────────────────────────────────────┐
│                   Game                      │
├────────────────────────────────────────────┤
│ - board: Board                         ◆   │  ← composition
│ - players: List<Player>                ◆   │  ← composition
│ - currentPlayerIndex: int                  │
│ - winStrategy: WinStrategy                 │  ← dependency (injected)
│ - gameStatus: GameStatus                   │
├────────────────────────────────────────────┤
│ + play(): void                             │
└────────────────────────────────────────────┘
          │ uses
          ↓
┌──────────────────────────────────────────┐
│  «interface»                              │
│  WinStrategy                             │
├──────────────────────────────────────────┤
│ + checkWin(board, lastRow,               │
│            lastCol, symbol): boolean     │
└──────────────────────────────────────────┘
          ▲
          │ implements
┌──────────────────────────────────────────┐
│  StandardWinStrategy                      │
│  (checks row / col / diagonals)           │
└──────────────────────────────────────────┘
```

---

## Relationships Summary

| From → To | Type | Why |
|---|---|---|
| Board → Cell | **Composition ◆** | Cells have no meaning outside a Board |
| Game → Board | **Composition ◆** | Board is owned and managed by Game |
| Game → Player | **Composition ◆** | Players are part of the game session |
| Game → WinStrategy | **Dependency** | Injected via constructor; Game uses it, doesn't own it |
| StandardWinStrategy → WinStrategy | **Realization ◁---** | implements interface |

---

## Win-Check Logic (StandardWinStrategy)

```
After each move at (lastRow, lastCol):
  1. Check entire lastRow           → all cells == symbol?
  2. Check entire lastCol           → all cells == symbol?
  3. If lastRow == lastCol          → check main diagonal
  4. If lastRow + lastCol == N-1    → check anti-diagonal

Optimization: only check diagonals if the last move lands on them.
This keeps win-check O(N) instead of checking all cells every turn.
```

---

## Key Design Decisions to Mention in Interview

1. **Strategy pattern** for win checking — swap `StandardWinStrategy` for custom rules (e.g., connect-K) without touching `Game`
2. **Circular player rotation** via `(currentPlayerIndex + 1) % players.size()` — supports 2+ players with zero extra code
3. **Win check on last move only** — no need to scan entire board; check only the row/col/diagonal of the last played cell
4. **Cell stores `char`** — allows any symbol (X, O, #, @), not just a boolean; no enum needed
5. **Invalid move retries same player** — `makeMove` returns boolean; loop continues without advancing `currentPlayerIndex`
