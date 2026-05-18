# LLD — Low Level Design Prep

Structured prep for senior fullstack interviews (FAANG OOD + Indian unicorn machine-coding rounds).

## Layout

| Folder | Topic | Contents |
|---|---|---|
| `00_syllabus/` | Syllabus + problem list | Full syllabus, brief syllabus, problems list, analysis |
| `01_OOP/` | OOP fundamentals | Encapsulation, Abstraction, Inheritance vs Composition, Polymorphism, Interface vs Abstract Class |
| `02_SOLID/` | SOLID principles | SRP, OCP, LSP, ISP, DIP — each with bad + good Java examples |
| `03_UML/` | UML basics | Class/sequence diagram theory |
| `04_Design_Patterns/` | GoF patterns | Strategy, Observer, Factory, Singleton, State, Builder, Decorator |
| `05_Problems/` | Machine-coding problems | parking_lot, book_my_show, elevator_system, lru_cache, snake_and_ladder, splitwise, tic_tac_toe, digital_wallet, vehicle_rental_system |

## Convention inside each section

- `theory/` — markdown notes
- `code/` — Java source organized by sub-topic
- Bad-vs-good pairs live as `code/<topic>/bad/` and `code/<topic>/good/`

## Convention inside each problem (`05_Problems/<name>/`)

- `problem.md` — requirements & clarifying Q&A
- `UML.md` — class diagram + entity relationships
- `design_notes.md` — flow, concurrency handling, design decisions (optional)
- `code/` — runnable Java: `Solution.java` entry + `domain/`, `enums/`, `service/`, `strategies/`

## Recommended order

1. Skim `00_syllabus/syllabus_brief.md`
2. `01_OOP` → `02_SOLID` → `03_UML` → `04_Design_Patterns` (theory + code each)
3. Practice `05_Problems/` — start with `parking_lot`, then progressively harder
