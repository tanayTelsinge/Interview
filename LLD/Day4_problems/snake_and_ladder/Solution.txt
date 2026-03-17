- Game orchestrates the loop: holds Board, List<Player>, DiceStrategy.
- Board: size N, Map<Integer,Integer> cellEffects (snake heads + ladder bottoms → destinations).
  resolvePosition(pos) = cellEffects.getOrDefault(pos, pos)
- Snake: head (higher pos) → tail (lower pos).
- Ladder: bottom (lower pos) → top (higher pos).
- Player: name, position (starts at 0).
- DiceStrategy (interface): roll() → int. StandardDice uses Random(1-6).
- GameStatus enum: IN_PROGRESS, FINISHED.


Flow:
Game starts → Board(size, snakes, ladders) → snakes+ladders collapsed into cellEffects map
  Loop (while !gameOver):
    → current player rolls dice
    → newPosition = currentPosition + roll
    → if newPosition > boardSize → overshoot, stay put (skip turn)
    → else resolvedPosition = board.resolvePosition(newPosition)
        → resolvedPos < newPos → SNAKE (slide down)
        → resolvedPos > newPos → LADDER (climb up)
        → resolvedPos == newPos → normal move
    → player.setPosition(resolvedPosition)
    → if resolvedPosition == boardSize → winner, gameOver = true
    → advance player: (index + 1) % players.size()


Design Patterns used:

- Strategy - DiceStrategy (StandardDice)
  - Swap StandardDice for LoadedDice (test) or TwoDice (variant) without touching Game


SOLID Principles:

- SRP  - Board resolves positions, Game orchestrates turns, Dice rolls — each one job
- OCP  - New dice variant = new DiceStrategy impl, Game unchanged
         New board effect (wormhole?) = update Board constructor + cellEffects map
- LSP  - DiceStrategy: any impl substitutable in Game
- DIP  - Game depends on DiceStrategy interface, not StandardDice directly
- ISP  - DiceStrategy has single method; no bloated interface


Key decisions worth mentioning in interview:

1. cellEffects map unifies snakes and ladders — Board.resolvePosition() is a single O(1) lookup.
   Game never imports Snake or Ladder classes at all.

2. Overshoot rule: if newPos > boardSize, player stays — must land EXACTLY on last cell.

3. DiceStrategy makes the game deterministic in tests:
   new Game(board, players, () -> 6)  ← lambda works as LoadedDice

4. No separate WinChecker needed — win condition is trivially pos == boardSize.


Known limitations / future scope:

- No concurrency needed (single-threaded turn-based game)
- No persistence / replay
- AI player: implement DiceStrategy to return optimal roll (or add BotPlayer strategy)
