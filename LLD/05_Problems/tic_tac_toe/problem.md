====================================================
  TIC TAC TOE — LLD Problem Statement
====================================================

Design and implement a Tic Tac Toe game.

----------------------------------------------------
FUNCTIONAL REQUIREMENTS
----------------------------------------------------
1. Played on an N×N board (configurable, default 3×3)
2. Supports 2 or more players, each with a unique symbol (X, O, #, etc.)
3. Players take turns marking a cell
4. A player WINS if they fill an entire row, column, or either diagonal with their symbol
5. Game ends in a DRAW if board is full with no winner
6. A player cannot mark an already-occupied cell — should prompt retry
7. Display the board state after every move

----------------------------------------------------
NON-FUNCTIONAL / CLARIFYING QUESTIONS
----------------------------------------------------
- Can more than 2 players play?          → Yes, N players supported
- Is the board always square?            → Yes, N×N
- Do we need AI player support?          → Human only for now, design should allow AI
- Is the game replayable?                → Nice to have, not required

----------------------------------------------------
OUT OF SCOPE
----------------------------------------------------
- Persistence / saving game state
- Network multiplayer
- AI / minimax strategy

----------------------------------------------------
FLOW
----------------------------------------------------
Game starts → Players added → Board initialized (N×N, all cells empty)
  Loop:
    → Print board
    → Current player inputs (row, col)
    → Validate: in bounds? not occupied?
    → Place symbol on cell
    → Check win condition (row / col / diagonal)
    → Check draw condition (board full, no winner)
    → Switch to next player
  Game ends → Announce winner or draw

----------------------------------------------------
ENTITIES TO THINK ABOUT
----------------------------------------------------
- Board         : holds N×N grid of Cells, printBoard(), makeMove()
- Cell          : row, col, symbol (null if empty)
- Player        : name, symbol
- Game          : orchestrates loop, holds players + board, tracks current player
- WinChecker    : interface — checkWin(board, lastMove) : boolean  [Strategy]
- GameStatus    : enum — IN_PROGRESS, WIN, DRAW

----------------------------------------------------
PATTERNS APPLICABLE
----------------------------------------------------
- Strategy : WinChecker (standard win vs custom rules)
- Iterator : cycling through players (circular)
