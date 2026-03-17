# Snake and Ladder — UML Class Diagram

> Matches the actual implementation in Day4_problems/snake_and_ladder/code/

---

## Class Diagram

```
«enumeration»
GameStatus
──────────
IN_PROGRESS
FINISHED


┌──────────────────┐     ┌──────────────────┐
│      Snake        │     │      Ladder       │
├──────────────────┤     ├──────────────────┤
│ - head: int       │     │ - bottom: int     │
│ - tail: int       │     │ - top: int        │
│  (head > tail)    │     │  (top > bottom)   │
├──────────────────┤     ├──────────────────┤
│ + getHead()       │     │ + getBottom()     │
│ + getTail()       │     │ + getTop()        │
└──────────────────┘     └──────────────────┘


┌──────────────────────────────────────────────┐
│                   Board                       │
├──────────────────────────────────────────────┤
│ - size: int                                   │
│ - cellEffects: Map<Integer, Integer>          │
│   (landing pos → final pos, snakes+ladders)  │
├──────────────────────────────────────────────┤
│ + resolvePosition(pos): int                  │
│ + getSize(): int                             │
└──────────────────────────────────────────────┘


┌──────────────────────┐
│        Player         │
├──────────────────────┤
│ - name: String        │
│ - position: int       │  (starts at 0)
├──────────────────────┤
│ + getName(): String   │
│ + getPosition(): int  │
│ + setPosition(int)    │
└──────────────────────┘


┌────────────────────────────────────────────┐
│                   Game                      │
├────────────────────────────────────────────┤
│ - board: Board                         ◆   │  ← composition
│ - players: List<Player>                ◆   │  ← composition
│ - dice: DiceStrategy                       │  ← dependency (injected)
│ - currentPlayerIndex: int                  │
│ - gameOver: boolean                        │
├────────────────────────────────────────────┤
│ + play(): void                             │
└────────────────────────────────────────────┘
          │ uses
          ↓
┌─────────────────────────┐
│  «interface»             │
│  DiceStrategy            │
├─────────────────────────┤
│ + roll(): int            │
└─────────────────────────┘
          ▲
          │ implements
┌─────────────────────────┐
│   StandardDice           │
│   (Random, returns 1–6)  │
└─────────────────────────┘
```

---

## Relationships Summary

| From → To | Type | Why |
|---|---|---|
| Game → Board | **Composition ◆** | Board is owned and managed by Game |
| Game → Player | **Composition ◆** | Players belong to the game session |
| Game → DiceStrategy | **Dependency** | Injected via constructor; swappable |
| StandardDice → DiceStrategy | **Realization ◁---** | implements interface |
| Board ← Snake/Ladder | **Dependency** | Passed at construction to build cellEffects map |

---

## Board cellEffects Map (key design decision)

```
Instead of storing List<Snake> and List<Ladder> separately in Board,
both are collapsed into a single Map<Integer, Integer>:

  snake head    → snake tail    (position goes DOWN)
  ladder bottom → ladder top    (position goes UP)

resolvePosition(pos) = cellEffects.getOrDefault(pos, pos)

Game never imports Snake or Ladder — Board fully encapsulates all effects.
OCP: add new effect type (wormhole?) = update Board constructor only.
```

---

## Move Resolution Flow

```
roll dice → newPos = currentPos + roll
  │
  ├── newPos > boardSize  →  overshoot: stay at currentPos
  │
  └── newPos <= boardSize →  resolvedPos = board.resolvePosition(newPos)
                               │
                               ├── resolvedPos < newPos  →  SNAKE (slide down)
                               ├── resolvedPos > newPos  →  LADDER (climb up)
                               └── resolvedPos == newPos →  normal move
                             update player.position = resolvedPos
                             if resolvedPos == boardSize → winner!
```

---

## Key Design Decisions to Mention in Interview

1. **Single `cellEffects` map** in Board — collapses snake + ladder logic into one `getOrDefault` call; Game never imports Snake or Ladder
2. **Strategy for Dice** — swap `StandardDice` for `LoadedDice(fixedValue)` in tests, or `TwoDice` without changing `Game`
3. **Overshoot rule**: `newPosition > board.getSize()` → skip move; player waits for a smaller roll
4. **Circular player rotation**: `(index + 1) % players.size()` — handles 2+ players
5. **No WinChecker needed** — win condition is `position == boardSize`, trivial inline check
