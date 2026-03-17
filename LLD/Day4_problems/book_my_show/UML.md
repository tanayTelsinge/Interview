# BookMyShow — UML Class Diagram

> Matches the actual implementation in Day4_problems/book_my_show/code/

---

## Class Diagram

```
«enumeration»                «enumeration»         «enumeration»        «enumeration»
SeatType                     SeatStatus            BookingStatus        PaymentStatus
────────────                 ──────────            ─────────────        ─────────────
SILVER(price=100)            AVAILABLE             CONFIRMED            SUCCESS
GOLD(price=200)              LOCKED                CANCELLED            FAILED
PLATINUM(price=500)          BOOKED                PAYMENT_FAILED
+ getPrice(): double


┌──────────────────────┐     ┌──────────────────────────┐
│        Movie          │     │          Seat             │
├──────────────────────┤     ├──────────────────────────┤
│ - movieId            │     │ - seatNumber: String      │
│ - title              │     │ - seatType: SeatType      │
│ - durationMins       │     │ - row: int                │
│ - genre              │     │ - col: int                │
└──────────────────────┘     └──────────────────────────┘


┌──────────────────────────────┐
│           Screen              │
├──────────────────────────────┤
│ - screenId                    │
│ - screenNumber: int           │
│ - seats: List<Seat>      ◆   │  ← composition
└──────────────────────────────┘


┌──────────────────────────────────────┐
│              Theatre                  │
├──────────────────────────────────────┤
│ - theatreId                           │
│ - name                                │
│ - city                                │
│ - screens: List<Screen>          ◆   │  ← composition
└──────────────────────────────────────┘


┌──────────────────────────────────────────────────────────┐
│                         Show                              │
├──────────────────────────────────────────────────────────┤
│ - showId                                                  │
│ - movie: Movie                                            │
│ - screen: Screen                                          │
│ - theatre: Theatre                                        │
│ - startTime: LocalDateTime                               │
│ - seatStatusMap: Map<String, SeatStatus>                 │
│   (seatNumber → SeatStatus, initialised from screen)    │
├──────────────────────────────────────────────────────────┤
│ + lockSeats(seatNumbers): boolean    [synchronized]      │
│ + confirmSeats(seatNumbers)          [synchronized]      │
│ + releaseSeats(seatNumbers)          [synchronized]      │
│ + getAvailableSeats(): List<Seat>                        │
└──────────────────────────────────────────────────────────┘


┌───────────────────┐     ┌──────────────────────────────────────┐
│      User          │     │              Booking                  │
├───────────────────┤     ├──────────────────────────────────────┤
│ - userId           │     │ - bookingId                           │
│ - name             │     │ - user: User                          │
│ - email            │     │ - show: Show                          │
└───────────────────┘     │ - seatNumbers: List<String>           │
                          │ - totalAmount: double                  │
                          │ - status: BookingStatus               │
                          ├──────────────────────────────────────┤
                          │ + setStatus(BookingStatus)            │
                          └──────────────────────────────────────┘


┌──────────────────────────────────────┐
│              Payment                  │
├──────────────────────────────────────┤
│ - paymentId                           │
│ - bookingId                           │
│ - amount: double                      │
│ - status: PaymentStatus              │
└──────────────────────────────────────┘


┌───────────────────────────────────────────────────────┐
│                   BookingService                       │
├───────────────────────────────────────────────────────┤
│ - paymentService: PaymentService                      │
├───────────────────────────────────────────────────────┤
│ + bookSeats(user, show, seatNumbers): Booking         │
│ + cancelBooking(booking): void                        │
│ - calculateAmount(show, seatNumbers): double          │
└───────────────────────────────────────────────────────┘
          │ uses
          ↓
┌─────────────────────────────────┐
│         PaymentService           │
├─────────────────────────────────┤
│ + processPayment(               │
│     bookingId, amount): Payment │
└─────────────────────────────────┘


┌───────────────────────────────────────────────────────┐
│                    ShowService                         │
├───────────────────────────────────────────────────────┤
│ - theatres: List<Theatre>                             │
│ - shows: List<Show>                                   │
├───────────────────────────────────────────────────────┤
│ + addTheatre(theatre)                                 │
│ + addShow(show)                                       │
│ + searchShows(movieTitle, city): List<Show>           │
└───────────────────────────────────────────────────────┘


┌──────────────────────────────────────────────┐
│  «interface»                                  │
│  SeatSelectionStrategy                        │
├──────────────────────────────────────────────┤
│ + selectSeats(show, count, seatType)          │
│   : List<String>                              │
└──────────────────────────────────────────────┘
          ▲
          │ implements
┌──────────────────────────────────────────────┐
│  RandomSeatSelectionStrategy                  │
│  (first N available seats of given SeatType)  │
└──────────────────────────────────────────────┘
```

---

## Relationships Summary

| From → To | Type | Why |
|---|---|---|
| Theatre → Screen | **Composition ◆** | Screens cannot exist without the Theatre |
| Screen → Seat | **Composition ◆** | Seats are physical parts of a Screen |
| Show → Movie | **Association** | Show references Movie; Movie exists independently |
| Show → Screen | **Association** | Show runs in a Screen; Screen exists independently |
| Booking → Show | **Association** | Booking references Show |
| Booking → User | **Association** | Booking references User |
| BookingService → PaymentService | **Dependency** | Injected via constructor |
| BookingService → SeatSelectionStrategy | **Dependency** | Injected at call site |

---

## Seat State Machine

```
         book()          payment SUCCESS
AVAILABLE ──────► LOCKED ───────────────► BOOKED
    ▲                │
    │                │ payment FAILED / cancelBooking()
    └────────────────┘
       releaseSeats()
```

---

## Concurrency: Why `synchronized` on `lockSeats()`

```
Thread A: lockSeats(["G1", "G2"])
Thread B: lockSeats(["G1", "G2"])   ← concurrent

Without sync:
  A reads G1=AVAILABLE ✓
  B reads G1=AVAILABLE ✓   ← both pass check!
  A writes G1=LOCKED
  B writes G1=LOCKED        ← double booking!

With synchronized:
  A acquires lock → checks all AVAILABLE → marks LOCKED → releases lock
  B acquires lock → finds G1=LOCKED → returns false → exception thrown ✓
```

---

## Key Design Decisions to Mention in Interview

1. **`synchronized` on `lockSeats()`** — check-then-act must be atomic; `ConcurrentHashMap` alone is insufficient
2. **3-state SeatStatus** (AVAILABLE → LOCKED → BOOKED) — LOCKED holds seats during payment; without it, two users pass the check simultaneously
3. **Price in `SeatType` enum** — correct for flat per-type pricing; move to `Map<SeatType, Double>` in Show if prices vary per show
4. **Show owns `seatStatusMap`** — same physical Seat has independent status per Show; Seat is a stateless value object
5. **Strategy for seat selection** — swap `RandomSeatSelectionStrategy` for `NearestScreenStrategy` without touching `BookingService`
