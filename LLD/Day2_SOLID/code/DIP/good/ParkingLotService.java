package code.DIP.good;

/**
 * GOOD — DIP applied.
 *
 * ParkingLotService depends on ParkingRepository (abstraction), not any concrete class.
 * Implementation is INJECTED via constructor.
 *
 * Benefits:
 *  1. Swap InMemoryRepo → DatabaseRepo → zero changes to ParkingLotService
 *  2. Unit test with a mock ParkingRepository — no real storage needed
 *  3. Spring Boot: @Autowired constructor injection wires this automatically
 */
public class ParkingLotService {

    private final ParkingRepository repo;  // depends on abstraction

    // Dependency injected — doesn't create it
    public ParkingLotService(ParkingRepository repo) {
        this.repo = repo;
    }

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

class DIPDemo {
    public static void main(String[] args) {
        // Production: inject in-memory repo
        ParkingRepository repo = new InMemoryParkingRepository();
        ParkingLotService service = new ParkingLotService(repo);

        service.parkVehicle("A1", "MH12AB1234");
        service.parkVehicle("A2", "MH12CD5678");
        service.removeVehicle("A1");

        // Testing: inject a mock or different impl — ParkingLotService is unchanged
        // ParkingLotService testService = new ParkingLotService(new MockParkingRepository());

        // Spring Boot equivalent:
        // @Service class ParkingLotService { @Autowired ParkingRepository repo; }
        // Spring resolves InMemoryParkingRepository (or DB impl) automatically
    }
}
