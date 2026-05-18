# 02 — SOLID Principles

Each principle has a `bad/` violation and a `good/` refactor under `code/`.

| Principle | Folder | Example |
|---|---|---|
| **S**ingle Responsibility | `code/SRP/` | God-class `BookingService` → split into `Booking`/`Payment`/`Notification` services |
| **O**pen-Closed | `code/OCP/` | Switch-on-type `PaymentProcessor` → `PaymentMethod` interface + strategies |
| **L**iskov Substitution | `code/LSP/` | `Penguin extends Bird` with throwing `fly()` → split `Bird`/`FlyingBird` |
| **I**nterface Segregation | `code/ISP/` | Fat `Worker` interface → `Workable` + `Feedable` |
| **D**ependency Inversion | `code/DIP/` | Service `new`s DB directly → injected `ParkingRepository` interface |

Full notes in `theory/SOLID_Principles.md`.
