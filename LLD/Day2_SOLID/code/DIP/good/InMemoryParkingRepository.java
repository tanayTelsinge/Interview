package code.DIP.good;

import java.util.HashMap;
import java.util.Map;

// Low-level module — implements the abstraction
public class InMemoryParkingRepository implements ParkingRepository {

    private final Map<String, String> spots = new HashMap<>();

    @Override
    public void reserveSpot(String spotId, String vehicleId) {
        spots.put(spotId, vehicleId);
    }

    @Override
    public void freeSpot(String spotId) {
        spots.remove(spotId);
    }

    @Override
    public boolean isAvailable(String spotId) {
        return !spots.containsKey(spotId);
    }
}
