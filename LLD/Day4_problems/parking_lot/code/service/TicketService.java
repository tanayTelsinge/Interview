package Day4_problems.parking_lot.code.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import Day4_problems.parking_lot.code.domain.ParkingSpot;
import Day4_problems.parking_lot.code.domain.Ticket;
import Day4_problems.parking_lot.code.domain.Vehicle;
import Day4_problems.parking_lot.code.strategies.pricingstrategy.PricingStrategy;

public class TicketService {
    

    private PricingStrategy pricingStrategy;

    public TicketService(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }
    public Ticket generateTicket(Vehicle vehicle, ParkingSpot spot) {
        String ticketId = "TICKET_" + System.currentTimeMillis() + "_" + vehicle.getVehicleNumber() + "_" + spot.getSpotNumber();
        return new Ticket(ticketId, vehicle.getVehicleNumber(), spot.getFloorNumber(), spot.getSpotNumber());
    }

    public double calculateHourlyFee(Ticket ticket) {
        long hoursParked = ChronoUnit.HOURS.between(ticket.getEntryDateTime(), LocalDateTime.now());
        return pricingStrategy.calculatePrice((int) hoursParked);
    }
}
