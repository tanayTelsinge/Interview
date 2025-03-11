
//This eg. covers abstraction, encapsulation, inheritance, polymorphism.
//Also covers SRP, OCP.
public class ParkingLot {
    public static abstract class Vehicle {
        private final String licensePlate; //encapsulation - data hiding

        public Vehicle(String licensePlate) {
            this.licensePlate = licensePlate;
        }

        public String getLicensePlate() { //encapsulation
            return licensePlate;
        }

        public abstract int parkingSpaceRequired(); // abstraction
    }

    public static class SportsVehicle extends Vehicle {

        public SportsVehicle(String licensePlate) {
            super(licensePlate);
        }
        @Override
        public int parkingSpaceRequired() {
            return 1;
        }
    }

    public static class HeavyVehicle extends Vehicle { //inheritance

        public HeavyVehicle(String licensePlate) {
            super(licensePlate);
        }

        @Override //polymorphism
        public int parkingSpaceRequired() {
            return 2;
        }
    }

    public static void main(String[] args) {

        Vehicle myFerrari = new SportsVehicle("MH12 XV8080");
        System.out.println(myFerrari.parkingSpaceRequired() + " parking space alloted for " + myFerrari.getLicensePlate());
    }
}