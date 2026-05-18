- ShowService: registry of theatres + shows. searchShows(movieTitle, city).
- BookingService: bookSeats() — lock → payment → confirm/release. cancelBooking().
- PaymentService: processPayment() — simulated, returns Payment with PaymentStatus.
- Show: owns Map<seatNumber, SeatStatus>. lockSeats(), confirmSeats(), releaseSeats() are synchronized.
- SeatType enum holds price (SILVER=100, GOLD=200, PLATINUM=500) — no separate pricing class needed.
- SeatSelectionStrategy (interface): selectSeats(show, count, seatType).
  RandomSeatSelectionStrategy: first N available seats of given type.


Flow:
searchShows(movie, city) → filtered from ShowService.shows list
  User picks show → seatStrategy.selectSeats() → list of seatNumbers
  bookSeats(user, show, seatNumbers):
    → show.lockSeats()   [synchronized — atomic check+lock, returns false if any unavailable]
    → if not locked → throw "seats unavailable"
    → calculateAmount() [build seatMap from screen, sum prices]
    → paymentService.processPayment()
    → SUCCESS  → show.confirmSeats()  → Booking(CONFIRMED)
    → FAILED   → show.releaseSeats()  → Booking(PAYMENT_FAILED)
  cancelBooking(booking):
    → show.releaseSeats(seatNumbers)
    → booking.setStatus(CANCELLED)


Concurrency handling:
  Problem: User A and User B both see Seat G1 as AVAILABLE and try to book simultaneously.
  Fix: Show.lockSeats() is synchronized.
    - Thread A acquires the lock, checks all seats AVAILABLE, marks them LOCKED, releases lock.
    - Thread B acquires the lock, finds G1 LOCKED, returns false → exception thrown.
  Key: check-then-act is inside a single synchronized block — cannot be split.


Design Patterns:

- Strategy : SeatSelectionStrategy — swap random for nearest/cheapest without touching BookingService
- State    : SeatStatus (AVAILABLE → LOCKED → BOOKED / AVAILABLE) — clear state transitions


SOLID Principles:

- SRP  : ShowService=search, BookingService=booking flow, PaymentService=payment, Show=seat state
- OCP  : New seat selection algorithm = new SeatSelectionStrategy impl, BookingService unchanged
- LSP  : SeatSelectionStrategy: any impl substitutable
- DIP  : BookingService depends on PaymentService (can be interface for mocking in tests)
- ISP  : SeatSelectionStrategy is focused single-method interface


Key decisions worth mentioning in interview:

1. synchronized on lockSeats() — the ONLY correct fix for double-booking.
   ConcurrentHashMap alone is NOT enough because check-then-act must be atomic.

2. SeatStatus has 3 states (not 2): AVAILABLE → LOCKED → BOOKED
   LOCKED is critical: seats are held during payment processing.
   Without LOCKED, two users could both pass the availability check.

3. Price stored in SeatType enum — avoids a separate pricing class for a flat-rate system.
   If prices vary per show, move to a Map<SeatType, Double> in Show.

4. Show stores Map<seatNumber, SeatStatus> — same physical seat has different status per show.
   Seat object is just a value object (physical seat data), stateless per show.


Known limitations / future scope:

- Lock timeout: if payment hangs, seats stay LOCKED indefinitely → add lock expiry with scheduled job
- Concurrent cancellation: cancelBooking should also be synchronized
- Payment interface: PaymentService should be an interface to allow gateway swapping
