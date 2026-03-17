- ParkingLot class: park(), vacateSpot(), getAvailableSpotsForVehicleType(), holds floors + strategies.
- ParkingFloor: holds list of ParkingSpots.
- ParkingSpot: tracks SpotStatus (AVAILABLE/OCCUPIED), SpotType (COMPACT/MEDIUM/LARGE), park(), vacateSpot().
- Ticket: ticketId, vehicleNumber, floorNumber, spotNumber, entryTime, exitTime.
- TicketService: generateTicket(), calculateHourlyFee() using PricingStrategy.
- Vehicle (abstract): vehicleNumber, VehicleType. Subclasses: Bike, Car, Truck.


Flow:
Vehicle enters lot
 - parkVehicle(vehicle)
     - SpotAllocationStrategy finds first available compatible spot (filters by SpotType + SpotStatus.AVAILABLE)
     - TicketService generates ticket (records entryTime = now)
     - spot.park(vehicle) marks spot OCCUPIED

Vehicle exits
 - vacateSpot(ticket)
     - match spot by floorNumber + spotNumber (both needed — spotNumber alone is not unique across floors)
     - TicketService.calculateHourlyFee() uses ChronoUnit.HOURS.between(entryTime, now)
     - spot.vacateSpot() marks spot AVAILABLE


Design Patterns used:

- Strategy - SpotAllocationStrategy (NearestSpotStrategy), PricingStrategy (HourlyPricingStrategy)
  - Allows swapping allocation/pricing logic without touching ParkingLot
- No Singleton - switched to constructor injection for testability


SOLID Principles:

- SRP  - each class has one responsibility (Ticket = data, TicketService = ticket ops, ParkingLot = orchestration)
- OCP  - new spot strategies or pricing models can be added without modifying existing classes
- LSP  - Vehicle is abstract; Bike/Car/Truck are substitutable anywhere Vehicle is used
- DIP  - ParkingLot depends on SpotAllocationStrategy and TicketService interfaces/abstractions, not concrete classes
- ISP  - not applicable here (interfaces are small and focused already)


Known limitations / future scope:

- Concurrency: parkVehicle and vacateSpot are not synchronized; race condition possible under concurrent access
- Payment: fee is calculated and printed but no Payment/Receipt object exists
