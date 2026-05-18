====================================================
  SNAKE AND LADDER — LLD Problem Statement
====================================================

Design and implement a Snake and Ladder board game.

----------------------------------------------------
FUNCTIONAL REQUIREMENTS
----------------------------------------------------
1. Configurable board size (default 100 cells, numbered 1 to N)
2. Supports 2 or more players
3. Each player starts at position 0 (off-board) and must reach exactly position N to win
4. On each turn, a player rolls a dice (1–6) and moves forward
5. If a player lands on the head of a snake → slides down to the snake's tail
6. If a player lands on the bottom of a ladder → climbs up to the ladder's top
7. If a move overshoots position N → player stays at current position (no move)
8. First player to reach exactly N wins; game ends immediately
9. Display each move: player name, roll, old position, new position, and any snake/ladder effect

----------------------------------------------------
NON-FUNCTIONAL / CLARIFYING QUESTIONS
----------------------------------------------------
- Can a player win by overshooting?          → No, must land exactly on N
- Can snakes and ladders overlap?            → No, assume valid configuration input
- Is the dice always 1–6?                   → Yes, but design should allow custom dice
- Can there be multiple dice?               → Nice to have, keep single dice for now
- Do we need an AI player?                  → No, human simulation only

----------------------------------------------------
OUT OF SCOPE
----------------------------------------------------
- Persistence / saving game state
- Network multiplayer
- GUI rendering

----------------------------------------------------
FLOW
----------------------------------------------------
Game starts → Board configured with snakes + ladders → Players added
  Loop (while no winner):
    → Current player rolls dice
    → Calculate new position = current + roll
    → If new position > N → skip move (overshoot)
    → If new position == snake head → slide to snake tail
    → If new position == ladder bottom → climb to ladder top
    → Update player position
    → Print move details
    → Check if player reached N → declare winner
    → Advance to next player

----------------------------------------------------
ENTITIES TO THINK ABOUT
----------------------------------------------------
- Board        : size N, holds Map of snakes + ladders, resolvePosition(pos)
- Snake        : head (higher), tail (lower)
- Ladder       : bottom (lower), top (higher)
- Player       : name, currentPosition
- Dice         : roll() → random int 1-6            [Strategy candidate]
- Game         : orchestrates loop, holds board + players + dice

----------------------------------------------------
PATTERNS APPLICABLE
----------------------------------------------------
- Strategy : DiceStrategy — swap standard dice for loaded/custom dice
- No win-checker needed (win = position == N, no complex logic)
