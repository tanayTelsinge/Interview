# 05 — Machine-Coding Problems

Full runnable Java solutions in the unicorn machine-coding format.

## Per-problem layout

```
<problem>/
├── problem.md          requirements + clarifying Q&A
├── UML.md              class diagram
├── design_notes.md     flow, concurrency, decisions (some problems)
└── code/
    ├── Solution.java   entry point with demo
    ├── domain/         entities
    ├── enums/          enum types
    ├── service/        orchestration services
    └── strategies/     pluggable strategy implementations
```

## Status

| Problem | problem.md | UML.md | design_notes | code |
|---|---|---|---|---|
| parking_lot | ✓ | ✓ | ✓ | ✓ |
| book_my_show | ✓ | ✓ | ✓ | ✓ |
| elevator_system | ✓ | ✓ | ✓ | ✓ |
| splitwise | ✓ | ✓ | ✓ | ✓ |
| snake_and_ladder | ✓ | ✓ | ✓ | ✓ |
| tic_tac_toe | ✓ | ✓ | ✓ | ✓ |
| lru_cache | ✓ | ✓ | — | ✓ |
| digital_wallet | — | ✓ | — | ✓ |
| vehicle_rental_system | ✓ | — | — | — |

## Suggested practice order

1. `tic_tac_toe` / `lru_cache` — warm-up
2. `parking_lot` — the classic
3. `snake_and_ladder` → `elevator_system` → `splitwise`
4. `book_my_show` — concurrency-heavy
5. `digital_wallet` / `vehicle_rental_system` — extend on your own

Run: `cd <problem>/code && javac -d /tmp/out $(find . -name "*.java") && java -cp /tmp/out Solution`
