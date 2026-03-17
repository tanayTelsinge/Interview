package code.DIP.bad;

import java.util.HashMap;
import java.util.Map;

/**
 * BAD EXAMPLE — DIP Violated.
 *
 * ParkingLotService (high-level module) directly creates InMemoryParkingRepository
 * (low-level module). This couples the two tightly.
 *
 * Problems:
 *  1. Can't swap to DatabaseParkingRepository without modifying ParkingLotService
 *  2. Can't unit test ParkingLotService — it always uses the real in-memory store
 *  3. High-level policy (parking logic) is coupled to low-level detail (storage mechanism)
 */

// Low-level module
class InMemoryParkingRepository_Bad {
    private final Map<String, String> spots = new HashMap<>();

    public void reserveSpot(String spotId, String vehicleId) {
        spots.put(spotId, vehicleId);
    }

    public void freeSpot(String spotId) {
        spots.remove(spotId);
    }

    public boolean isAvailable(String spotId) {
        return !spots.containsKey(spotId);
    }
}

// High-level module — violates DIP by creating low-level module directly
public class ParkingLotService {

    // VIOLATION: directly creates the concrete implementation
    private final InMemoryParkingRepository_Bad repo = new InMemoryParkingRepository_Bad();

    public void parkVehicle(String spotId, String vehicleId) {
        if (!repo.isAvailable(spotId)) {
            throw new IllegalStateException("Spot " + spotId + " is already occupied");
        }
        repo.reserveSpot(spotId, vehicleId);
        System.out.println("Vehicle " + vehicleId + " parked at spot " + spotId);
    }

    public void removeVehicle(String spotId) {
        repo.freeSpot(spotId);
        System.out.println("Spot " + spotId + " is now free");
    }
}
// Can't test ParkingLotService with a mock repo — it's hardcoded
// Can't switch to DB storage — must modify ParkingLotService
