====================================================
  BOOKMYSHOW — LLD Problem Statement
====================================================

Design a movie ticket booking system.

----------------------------------------------------
FUNCTIONAL REQUIREMENTS
----------------------------------------------------
1. A Theatre has multiple Screens; each Screen runs multiple Shows
2. Each Show has seats of types: SILVER, GOLD, PLATINUM (with different prices)
3. Users can search shows by movie name + city
4. User selects seats and books a ticket
5. Booking is confirmed only after successful payment
6. A booked seat cannot be booked again
7. User can cancel a booking — seats return to AVAILABLE

----------------------------------------------------
NON-FUNCTIONAL / CLARIFYING QUESTIONS
----------------------------------------------------
- Two users booking same seat simultaneously?  → Yes, handle race condition
- Payment in scope?                            → Simulated (PaymentStatus SUCCESS/FAILED)
- Multiple seats per booking?                  → Yes
- Seat prices fixed per type or per show?      → Fixed per SeatType
- Waitlisting?                                 → No, out of scope

----------------------------------------------------
OUT OF SCOPE
----------------------------------------------------
- Real payment gateway
- Food/F&B ordering
- Recommendations / search ranking

----------------------------------------------------
FLOW
----------------------------------------------------
User searches movie + city → list of Shows
User picks Show → sees available seats
User selects seats:
  → seats LOCKED atomically (prevents double booking)
  → payment processed
  → SUCCESS → seats BOOKED, Booking CONFIRMED
  → FAILED  → seats released back to AVAILABLE

----------------------------------------------------
ENTITIES
----------------------------------------------------
- Movie       : movieId, title, durationMins, genre
- Seat        : seatNumber, SeatType (with price), row, col
- Screen      : screenId, screenNumber, List<Seat>
- Theatre     : theatreId, name, city, List<Screen>
- Show        : showId, movie, screen, theatre, startTime, Map<seatNumber, SeatStatus>
- User        : userId, name, email
- Booking     : bookingId, user, show, seatNumbers, totalAmount, BookingStatus
- Payment     : paymentId, bookingId, amount, PaymentStatus

----------------------------------------------------
ENUMS
----------------------------------------------------
- SeatType    : SILVER(100), GOLD(200), PLATINUM(500)
- SeatStatus  : AVAILABLE, LOCKED, BOOKED
- BookingStatus : CONFIRMED, CANCELLED, PAYMENT_FAILED
- PaymentStatus : SUCCESS, FAILED

----------------------------------------------------
PATTERNS APPLICABLE
----------------------------------------------------
- Strategy : SeatSelectionStrategy (random, nearest, cheapest first)
- State    : SeatStatus transitions AVAILABLE → LOCKED → BOOKED / AVAILABLE
