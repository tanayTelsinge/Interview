import java.util.ArrayList;
import java.util.List;

// ✅ Demonstrating SOLID + OOP
public class ParkingLot {
    public static void main(String[] args) {
        ParkingLotManager lotManager = new ParkingLotManager(5);

        Vehicle ferrari = new SportsCar("MH12 XV8080");
        Vehicle truck = new Truck("MH14 AB1234");
        Vehicle bike = new Motorcycle("MH01 XY7890");

        lotManager.parkVehicle(ferrari);
        lotManager.parkVehicle(truck);
        lotManager.parkVehicle(bike);

        lotManager.displayOccupiedSpaces();
    }
}

// ✅ 1. ISP - Interface Segregation Principle (Only essential methods)
// as licensePlate is instance variable (can be different per vehicle, its not here).
interface Vehicle {
    String getLicensePlate();
    int getParkingSpaceRequired();
}

// ✅ 2. OCP - Open-Closed Principle (Base class for code reusability)
abstract class BaseVehicle implements Vehicle {
    private final String licensePlate;

    public BaseVehicle(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    @Override
    public String getLicensePlate() {
        return licensePlate;
    }
}

// ✅ 3. LSP - Liskov Substitution Principle (Subclasses can replace BaseVehicle)
class SportsCar extends BaseVehicle {
    public SportsCar(String licensePlate) {
        super(licensePlate);
    }

    @Override
    public int getParkingSpaceRequired() {
        return 1;
    }
}

class Truck extends BaseVehicle {
    public Truck(String licensePlate) {
        super(licensePlate);
    }

    @Override
    public int getParkingSpaceRequired() {
        return 2;
    }
}

class Motorcycle extends BaseVehicle {
    public Motorcycle(String licensePlate) {
        super(licensePlate);
    }

    @Override
    public int getParkingSpaceRequired() {
        return 1;
    }
}

// ✅ 4. SRP - Single Responsibility Principle (Handles only parking logic)
class ParkingLotManager {
    private final int totalSpaces;
    private int occupiedSpaces;
    private final List<Vehicle> parkedVehicles;

    public ParkingLotManager(int totalSpaces) {
        this.totalSpaces = totalSpaces;
        this.occupiedSpaces = 0;
        this.parkedVehicles = new ArrayList<>();
    }

    // ✅ 5. DIP - Dependency Inversion Principle (Depends on Vehicle abstraction)
    public boolean parkVehicle(Vehicle vehicle) {
        int requiredSpaces = vehicle.getParkingSpaceRequired();
        if (occupiedSpaces + requiredSpaces > totalSpaces) {
            System.out.println("❌ Not enough space for " + vehicle.getLicensePlate());
            return false;
        }
        parkedVehicles.add(vehicle);
        occupiedSpaces += requiredSpaces;
        System.out.println("✅ Parked " + vehicle.getLicensePlate() + " - Spaces Used: " + requiredSpaces);
        return true;
    }

    public void displayOccupiedSpaces() {
        System.out.println("📊 Total Occupied Spaces: " + occupiedSpaces + "/" + totalSpaces);
    }
}


