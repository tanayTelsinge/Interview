# 04 — Design Patterns

Seven highest-yield GoF patterns for LLD interviews. Each has a `bad/` (the smell) and `good/` (the pattern applied), plus a `*Demo.java` you can run.

| # | Pattern | Type | Use Case in Examples |
|---|---|---|---|
| 01 | Strategy | Behavioral | Pluggable payment methods |
| 02 | Observer | Behavioral | Stock price → email/SMS subscribers |
| 03 | Factory | Creational | Notification channel creation |
| 04 | Singleton | Creational | Three thread-safe variants (DCL, Bill Pugh, Enum) |
| 05 | State | Behavioral | Order lifecycle (Pending→Confirmed→Shipped→Delivered) |
| 06 | Builder | Creational | HttpRequest with many optional fields |
| 07 | Decorator | Structural | Logging + caching wrappers around DataService |

Full theory: `theory/Design_Patterns_Masterclass.md`.

Run a demo: `cd code/01_Strategy && javac good/*.java bad/*.java StrategyDemo.java && java StrategyDemo`
