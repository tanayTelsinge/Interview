# BookMyShow — LLD Quick Reference

## Enums
| SeatType | SeatStatus | BookingStatus | PaymentStatus |
|---|---|---|---|
| SILVER(100) | AVAILABLE | CONFIRMED | SUCCESS |
| GOLD(200) | LOCKED | CANCELLED | FAILED |
| PLATINUM(500) | BOOKED | PAYMENT_FAILED | |

---

## Core Classes

```
Movie              Seat                Screen ◆─ Seat      Theatre ◆─ Screen
──────             ────                ──────────────       ─────────────────
movieId            seatNumber          screenId             theatreId
title              seatType            screenNumber         name, city
durationMins       row, col            seats: List<Seat>    screens: List<Screen>
genre

Show                                          User        Booking
────────────────────────────────────          ────        ───────────────────────
showId, movie, screen, theatre                userId      bookingId, user, show
startTime: LocalDateTime                      name        seatNumbers: List<String>
seatStatusMap: Map<String, SeatStatus>        email       totalAmount, status
+ lockSeats(seatNumbers)    [synchronized]
+ confirmSeats(seatNumbers) [synchronized]
+ releaseSeats(seatNumbers) [synchronized]
+ getAvailableSeats()

BookingService                    PaymentService         ShowService
──────────────────────────        ──────────────         ──────────────────────────
+ bookSeats(user,show,seats)  ──► + processPayment()    + addTheatre/addShow()
+ cancelBooking(booking)                                 + searchShows(title,city)

«interface» SeatSelectionStrategy
+ selectSeats(show, count, seatType): List<String>
  ▲ RandomSeatSelectionStrategy (first N available of given type)
```

---

## Seat State Machine
```
AVAILABLE ──lockSeats()──► LOCKED ──payment OK──► BOOKED
    ▲                         │
    └──payment FAIL/cancel────┘  releaseSeats()
```

---

## Concurrent Booking (Two Users, Same Seat)

**Problem:** Both read `G1=AVAILABLE` before either writes → double booking.

**Fix:** `lockSeats()` is `synchronized` on the `Show` object:
```
User A: acquires lock → seats AVAILABLE → marks LOCKED → releases lock
User B: acquires lock → finds G1=LOCKED → throws exception ✗
```
> `ConcurrentHashMap` alone is NOT enough — check-then-act must be atomic.

### Locking Strategies

| Strategy | How | When |
|---|---|---|
| **Pessimistic** (`synchronized`) | Block all threads upfront | Single JVM — our impl |
| **Optimistic** (`@Version`) | Let all try, retry on conflict | High read, rare conflicts |
| **DB row lock** (`SELECT FOR UPDATE`) | DB locks the row | Multi-server, DB-backed |
| **Redis distributed lock** (`SETNX + TTL`) | Only first caller gets key; TTL auto-releases | Multi-server, high scale |

```java
// 1. Pessimistic
public synchronized boolean lockSeats(List<String> seats) {
    if (seats.stream().anyMatch(s -> seatStatusMap.get(s) != AVAILABLE)) return false;
    seats.forEach(s -> seatStatusMap.put(s, LOCKED));
    return true;
}

// 2. Optimistic — @Version (JPA adds "AND version=5" to every UPDATE)
@Entity class Seat {
    @Version int version;  // DB column; JPA manages it, never set manually
}
// JPA generates: UPDATE seat SET status='LOCKED', version=6 WHERE id=1 AND version=5
// 0 rows updated → OptimisticLockException → retry
for (int i = 0; i < 3; i++) {
    try { seat.setStatus(LOCKED); repo.save(seat); break; }
    catch (OptimisticLockException e) { /* retry */ }
}

// 3. DB row lock
@Query("SELECT s FROM Seat s WHERE s.id = :id FOR UPDATE")
Seat findByIdForUpdate(@Param("id") Long id);  // DB holds row lock until tx commits

// 4. Redis distributed lock
String key = "lock:show:" + showId + ":seat:" + seatNo;
Boolean acquired = redis.opsForValue().setIfAbsent(key, userId, Duration.ofSeconds(300));
if (!acquired) throw new SeatAlreadyLockedException();
try { /* do booking */ } finally { redis.delete(key); }
```

> **Interview tip:** Start with `synchronized`, then say *"at scale with multiple nodes I'd use Redis `SETNX` or DB `SELECT FOR UPDATE`"*.

---

## Key Design Points
1. **`synchronized` on Show methods** — atomicity for check-then-act
2. **3-state SeatStatus** — LOCKED bridges the gap between selection and payment
3. **Show owns `seatStatusMap`** — same physical Seat is independent per Show (Seat is stateless)
4. **Price in SeatType enum** — flat pricing; move to `Map<SeatType,Double>` in Show for per-show pricing
5. **Strategy pattern** — swap seat selection algo without touching `BookingService`
