package Day4_problems.parking_lot.code.domain;

import Day4_problems.parking_lot.code.enums.SpotStatus;
import Day4_problems.parking_lot.code.enums.SpotType;
import Day4_problems.parking_lot.code.enums.VehicleType;
public class ParkingSpot {

    private int spotNumber;
    private Vehicle parkedVehicle;
    private SpotType spotType;
    private SpotStatus spotStatus = SpotStatus.AVAILABLE;
    private int floorNumber;

    public ParkingSpot(int spotNumber, int floorNumber, SpotType spotType) {
        this.spotNumber = spotNumber;
        this.floorNumber = floorNumber;
        this.spotType = spotType;
    }

    public SpotType getSpotType() {
        return spotType;
    }

    public VehicleType getVehicleType() {
        return parkedVehicle != null ? parkedVehicle.getVehicleType() : null;
    }

    public void setSpotType(SpotType spotType) {
        this.spotType = spotType;
    }

    public SpotStatus getSpotStatus() {
        return spotStatus;
    }

    public void park(Vehicle vehicle) {
        this.parkedVehicle = vehicle;
        this.spotStatus = SpotStatus.OCCUPIED;
    }

    public int getSpotNumber() {
        return spotNumber;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void vacateSpot() {
        this.parkedVehicle = null;
        this.spotStatus = SpotStatus.AVAILABLE;
    }

}
