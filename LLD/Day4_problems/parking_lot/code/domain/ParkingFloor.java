package Day4_problems.parking_lot.code.domain;

import java.util.List;

public class ParkingFloor {
    
    private List<ParkingSpot> spots;

    private int floorNumber;

    public ParkingFloor(int floorNumber, List<ParkingSpot> spots) {
        this.floorNumber = floorNumber;
        this.spots = spots;
    }

    public List<ParkingSpot> getSpots() {
        return spots;
    }
}
