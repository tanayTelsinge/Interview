package Day4_problems.parking_lot.code.domain;

import java.util.List;

import Day4_problems.parking_lot.code.enums.SpotStatus;
import Day4_problems.parking_lot.code.enums.SpotType;
import Day4_problems.parking_lot.code.enums.VehicleType;
import Day4_problems.parking_lot.code.service.TicketService;
import Day4_problems.parking_lot.code.strategies.spotallocationstrategy.SpotAllocationStrategy;

public class ParkingLot {

    private List<ParkingFloor> floors;

    private SpotAllocationStrategy spotAllocationStrategy;

    private TicketService ticketService;

    public ParkingLot(List<ParkingFloor> floors,SpotAllocationStrategy spotAllocationStrategy,TicketService ticketService) {
        this.ticketService = ticketService;
        this.floors = floors;
        this.spotAllocationStrategy = spotAllocationStrategy;
    }

    public Ticket parkVehicle(Vehicle vehicle) {
        VehicleType vehicleType = vehicle.getVehicleType();
        ParkingSpot spot = spotAllocationStrategy.allocateSpot(vehicleType, getAllSpots());
        Ticket ticket = ticketService.generateTicket(vehicle, spot);
        spot.park(vehicle);
        return ticket;
    }

    public void vacateSpot(Ticket ticket) {
        getAllSpots().stream()
                .filter(spot -> spot.getFloorNumber() == ticket.getFloorNumber() && spot.getSpotNumber() == ticket.getSpotNumber())
                .findFirst()
                .ifPresent(ParkingSpot::vacateSpot);
        double fee = ticketService.calculateHourlyFee(ticket);
        System.out.println("Fee" + " : Rs. " + fee + " paid and vacated");
    }

    private List<ParkingSpot> getAllSpots() {
        return floors.stream().flatMap(floor -> floor.getSpots().stream()).toList();
    }

    public long getAvailableSpotsForVehicleType(VehicleType vehicleType) {
        List<SpotType> compatibleSpots = spotAllocationStrategy.getCompatibleSpotTypes(vehicleType);
        return floors.stream().flatMap(floor -> floor.getSpots().stream())
                .filter(spot -> compatibleSpots.contains(spot.getSpotType()) && SpotStatus.AVAILABLE.equals(spot.getSpotStatus()))
                .count();
    }
}
