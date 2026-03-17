package Day4_problems.parking_lot.code;

import java.util.List;

import Day4_problems.parking_lot.code.domain.Bike;
import Day4_problems.parking_lot.code.domain.Car;
import Day4_problems.parking_lot.code.domain.ParkingFloor;
import Day4_problems.parking_lot.code.domain.ParkingLot;
import Day4_problems.parking_lot.code.domain.ParkingSpot;
import Day4_problems.parking_lot.code.domain.Ticket;
import Day4_problems.parking_lot.code.domain.Truck;
import Day4_problems.parking_lot.code.enums.SpotType;
import Day4_problems.parking_lot.code.enums.VehicleType;
import Day4_problems.parking_lot.code.service.TicketService;
import Day4_problems.parking_lot.code.strategies.pricingstrategy.HourlyPricingStrategy;
import Day4_problems.parking_lot.code.strategies.spotallocationstrategy.NearestSpotStrategy;

public class Solution {

    public static void main(String[] args) {
        ParkingLot parkingLot = initParkingLot();

        // Park vehicles
        Ticket ticketCar = parkingLot.parkVehicle(new Car("KA-01-HH-1234", VehicleType.CAR));
        Ticket ticketTruck = parkingLot.parkVehicle(new Truck("KA-01-HH-9999", VehicleType.TRUCK));
        Ticket ticketBike = parkingLot.parkVehicle(new Bike("KA-01-BB-0001", VehicleType.BIKE));

        System.out.println(parkingLot.getAvailableSpotsForVehicleType(VehicleType.BIKE));

        parkingLot.vacateSpot(ticketBike);

        System.out.println(parkingLot.getAvailableSpotsForVehicleType(VehicleType.BIKE));
    }

    private static ParkingLot initParkingLot() {
        // Create spots
        ParkingSpot compactSpot1 = new ParkingSpot(1, 1, SpotType.COMPACT);
        ParkingSpot compactSpot2 = new ParkingSpot(2, 1, SpotType.COMPACT);
        ParkingSpot mediumSpot1  = new ParkingSpot(3, 1, SpotType.MEDIUM);
        ParkingSpot mediumSpot2  = new ParkingSpot(4, 1, SpotType.MEDIUM);
        ParkingSpot largeSpot1   = new ParkingSpot(5, 1, SpotType.LARGE);

        ParkingSpot compactSpot3 = new ParkingSpot(6, 2, SpotType.COMPACT);
        ParkingSpot mediumSpot3  = new ParkingSpot(7, 2, SpotType.MEDIUM);
        ParkingSpot largeSpot2   = new ParkingSpot(8, 2, SpotType.LARGE);

        // Create floors
        ParkingFloor floor1 = new ParkingFloor(1, List.of(compactSpot1, compactSpot2, mediumSpot1, mediumSpot2, largeSpot1));
        ParkingFloor floor2 = new ParkingFloor(2, List.of(compactSpot3, mediumSpot3, largeSpot2));

        // Configure parking lot
        ParkingLot parkingLot = new ParkingLot(List.of(floor1, floor2), new NearestSpotStrategy(), new TicketService(new HourlyPricingStrategy(50)));
        return parkingLot;
    }
}
