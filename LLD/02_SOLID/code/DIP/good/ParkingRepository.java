package code.DIP.good;

/**
 * GOOD — DIP applied.
 *
 * ParkingRepository = the abstraction both high-level and low-level depend on.
 * ParkingLotService depends on this interface, not any concrete class.
 */
public interface ParkingRepository {
    void reserveSpot(String spotId, String vehicleId);
    void freeSpot(String spotId);
    boolean isAvailable(String spotId);
}
